package io.github.mdpearce.sonicswitcher.converter

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import io.github.mdpearce.sonicswitcher.converter.results.ConversionCancelled
import io.github.mdpearce.sonicswitcher.converter.results.ConversionComplete
import io.github.mdpearce.sonicswitcher.converter.results.ConversionException
import io.github.mdpearce.sonicswitcher.converter.results.ConversionResult
import io.github.mdpearce.sonicswitcher.converter.results.Processing
import io.github.mdpearce.sonicswitcher.converter.results.ProgressUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavNativeConverter(
    private val context: Context,
) : AudioFileConverter {
    companion object {
        private const val TAG = "WavNativeConverter"
        private const val TIMEOUT_US = 10000L
    }

    override suspend fun convertAudioFile(
        input: Uri,
        output: Uri,
        onProgressUpdated: (ProgressUpdate) -> Unit,
    ): ConversionResult =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            var fileOutputStream: java.io.FileOutputStream? = null
            var pfd: android.os.ParcelFileDescriptor? = null

            try {
                onProgressUpdated(Processing(0.0f))
                extractor.setDataSource(context, input, null)
                val numTracks = extractor.trackCount
                var audioTrackIndex = -1
                var format: MediaFormat? = null

                for (i in 0 until numTracks) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        format = trackFormat
                        break
                    }
                }

                if (audioTrackIndex < 0 || format == null) {
                    throw ConversionException("No audio track found in $input")
                }

                extractor.selectTrack(audioTrackIndex)

                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                pfd = context.contentResolver.openFileDescriptor(output, "rw")
                    ?: throw ConversionException("Cannot open output file")
                fileOutputStream = java.io.FileOutputStream(pfd.fileDescriptor)
                val fileChannel = fileOutputStream.channel
                fileChannel.position(0)

                // Write a dummy header first. 44 bytes.
                val dummyHeader = ByteBuffer.allocate(44)
                fileChannel.write(dummyHeader)

                var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                var isExtractorEOS = false
                var isCodecEOS = false
                var totalBytesWritten = 0L
                val durationUs =
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        format.getLong(MediaFormat.KEY_DURATION)
                    } else {
                        -1L
                    }

                val info = MediaCodec.BufferInfo()

                while (!isCodecEOS && isActive) {
                    if (!isExtractorEOS) {
                        val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inIndex >= 0) {
                            val buffer = codec.getInputBuffer(inIndex)
                            val sampleSize = buffer?.let { extractor.readSampleData(it, 0) } ?: -1
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                isExtractorEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                    when (outIndex) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = codec.outputFormat
                            sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // no-op
                        }
                        else -> {
                            if (outIndex >= 0) {
                                val outBuffer = codec.getOutputBuffer(outIndex)
                                if (outBuffer != null && info.size > 0) {
                                    // Extract the PCM data
                                    val chunk = ByteArray(info.size)
                                    outBuffer.position(info.offset)
                                    outBuffer.limit(info.offset + info.size)
                                    outBuffer.get(chunk)

                                    fileChannel.write(ByteBuffer.wrap(chunk))
                                    totalBytesWritten += info.size

                                    if (durationUs > 0) {
                                        val progress =
                                            (info.presentationTimeUs.toDouble() / durationUs).coerceIn(
                                                0.0,
                                                1.0,
                                            )
                                        onProgressUpdated(Processing(progress.toFloat()))
                                    }
                                }
                                codec.releaseOutputBuffer(outIndex, false)
                                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    isCodecEOS = true
                                }
                            }
                        }
                    }
                }

                if (!isActive) {
                    return@withContext ConversionCancelled
                }

                // Write real header
                fileChannel.position(0)
                val header = createWavHeader(totalBytesWritten, sampleRate, channelCount.toShort(), 16)
                fileChannel.write(ByteBuffer.wrap(header))

                return@withContext ConversionComplete
            } catch (e: Exception) {
                Log.e(TAG, "Error during conversion", e)
                throw ConversionException(e.message ?: "Unknown error")
            } finally {
                try {
                    codec?.stop()
                    codec?.release()
                } catch (_: Exception) {
                }
                try {
                    extractor.release()
                } catch (_: Exception) {
                }
                try {
                    fileOutputStream?.close()
                } catch (_: Exception) {
                }
                try {
                    pfd?.close()
                } catch (_: Exception) {
                }
            }
        }

    private fun createWavHeader(
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Short,
        bitRate: Short,
    ): ByteArray {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitRate / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen.toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size
        header.putShort(1) // AudioFormat, 1 = PCM
        header.putShort(channels)
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitRate / 8).toShort()) // BlockAlign
        header.putShort(bitRate)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(totalAudioLen.toInt())

        return header.array()
    }
}

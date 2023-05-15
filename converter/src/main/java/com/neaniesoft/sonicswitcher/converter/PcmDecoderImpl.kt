package com.neaniesoft.sonicswitcher.converter

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.File
import java.io.FileOutputStream

class PcmDecoderImpl(
    private val mediaExtractorFactory: () -> MediaExtractor,
    private val context: Context
) : PcmDecoder {

    companion object {
        private const val TIMEOUT_US = 10000L
    }

    override fun decodeToPcm(
        input: Uri,
        outputPath: String
    ): Result<File, ConverterError> {
        val mediaExtractor = mediaExtractorFactory()

        mediaExtractor.setDataSource(context, input, null)

        val track = (0 until mediaExtractor.trackCount).map { index ->
            TrackWithFormat(index, mediaExtractor.getTrackFormat(index))
        }.firstOrNull { track ->
            track.format.getString(MediaFormat.KEY_MIME, "").startsWith("audio/")
        } ?: return Err(NoAudioTrackError)

        mediaExtractor.selectTrack(track.index)

        val decoder =
            MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(track.format)
                ?: return Err(NoDecoderError)
        val codec = MediaCodec.createByCodecName(decoder)

        codec.configure(track.format, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        val outputFile = File(outputPath)
        val outputStream = FileOutputStream(outputFile)

        var sawInputEOS = false
        var sawOutputEOS = false

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val destBuffer =
                        codec.getInputBuffer(inputBufferIndex) ?: return Err(NoInputBufferError)
                    val sampleSize = mediaExtractor.readSampleData(destBuffer, 0)

                    val presentationTimeUs = if (sampleSize < 0) {
                        sawInputEOS = true
                        0L
                    } else {
                        mediaExtractor.sampleTime
                    }

                    codec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        if (sawInputEOS) {
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        } else {
                            0
                        }
                    )

                    if (!sawInputEOS) {
                        mediaExtractor.advance()
                    }
                }
            }

            val res = codec.dequeueOutputBuffer(info, TIMEOUT_US)

            if (res >= 0) {
                val buf = codec.getOutputBuffer(res) ?: return Err(NoOutputBufferError)
                val chunk = ByteArray(info.size)

                buf.get(chunk)
                buf.clear()
                if (chunk.isNotEmpty()) {
                    outputStream.write(chunk)
                }

                codec.releaseOutputBuffer(res, false)

                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                }
            }
        }
        outputStream.close()

        mediaExtractor.release()
        codec.stop()
        codec.release()

        return Ok(outputFile)
    }
}

data class TrackWithFormat(val index: Int, val format: MediaFormat)

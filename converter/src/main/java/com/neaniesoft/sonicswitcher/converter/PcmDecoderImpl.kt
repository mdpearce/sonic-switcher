package com.neaniesoft.sonicswitcher.converter

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class PcmDecoderImpl(
    private val mediaExtractorFactory: () -> MediaExtractor,
    private val context: Context
) : PcmDecoder {

    companion object {
        private const val TIMEOUT_US = 10000L
        private const val TAG = "PcmDecoderImpl"
    }

    override suspend fun decodeToPcm(
        input: Uri,
        outputPath: Uri
    ): Result<Boolean, ConverterError> {
        Log.d(TAG, "Decoding $input to $outputPath")
        val id = UUID.randomUUID().toString()
        val extractor = mediaExtractorFactory()

        Log.d(TAG, "Setting data source on extractor")
        extractor.setDataSource(context, input, null)

        val track = (0 until extractor.trackCount).map { index ->
            TrackWithFormat(index, extractor.getTrackFormat(index))
        }.firstOrNull { track ->
            track.format.getString(MediaFormat.KEY_MIME, "").startsWith("audio/")
        } ?: return Err(NoAudioTrackError)
        Log.d(TAG, "Selecting track ${track.index}")
        extractor.selectTrack(track.index)

        Log.d(TAG, "Finding decoder for ${track.format}")
        val decoder =
            MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(track.format)
                ?: return Err(NoDecoderError)

        Log.d(TAG, "Creating codec for $decoder")
        val codec = MediaCodec.createByCodecName(decoder)
        Log.d(TAG, "Configuring codec")
        codec.configure(track.format, null, null, 0)
        Log.d(TAG, "Starting codec")
        codec.start()

        val tmpFile = File(context.cacheDir, "$id.raw")
        val fos = FileOutputStream(tmpFile)
        val bos = BufferedOutputStream(fos, 65536)
        val dos = DataOutputStream(bos)

        val bufferInfo = MediaCodec.BufferInfo()

        var doneReading = false

        while (!doneReading) {
//            Log.d(TAG, "Getting inputBufferIndex")
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
//                Log.d(TAG, "inputBufferIndex is $inputBufferIndex, getting buffer")
                val buffer = codec.getInputBuffer(inputBufferIndex) ?: return Err(ProviderCrashed)
//                Log.d(TAG, "Reading sample data into buffer")
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    Log.d(TAG, "No more samples, indicating end of stream")
                    doneReading = true
                    codec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                } else {
//                    Log.d(TAG, "Queueing input buffer with sample size $sampleSize")
                    val presentationTimeUs = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
//                    Log.d(TAG, "Advancing extractor")
                    extractor.advance()
                }
            }

//            Log.d(TAG, "Getting output buffer index")
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0 || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (outputBufferIndex >= 0) {
//                    Log.d(TAG, "OutputBufferIndex is $outputBufferIndex, getting output buffer")
                    val buffer =
                        codec.getOutputBuffer(outputBufferIndex) ?: return Err(ProviderCrashed)
                    val shortBuffer = buffer.asShortBuffer()
//                    Log.d(TAG, "Writing short buffer to disk")
                    for (i in 0 until bufferInfo.size / 2) {
                        dos.writeShort(shortBuffer.get(i).toInt())
                    }
//                    Log.d(TAG, "Releasing output buffer")
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                } else {
                    val format = codec.outputFormat
                    Log.d("PcmDecoderImpl", "Format changed: $format")
                }
//                Log.d(TAG, "Getting output buffer index")
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            }
        }

        Log.d(TAG, "Stopping and releasing resources")
        codec.stop()
        codec.release()
        extractor.release()
        fos.close()
        bos.close()
        dos.close()

        Log.d(TAG, "Creating wav output resources")
        val rawPcmInputStream = FileInputStream(tmpFile)
        val wavFile = RandomAccessFile(File(context.cacheDir, "$id.wav"), "rw")

        val channels = track.format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = track.format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        Log.d(TAG, "Writing wav header")
        writeWavHeader(wavFile, channels.toShort(), 16.toShort(), sampleRate, rawPcmInputStream)

        Log.d(TAG, "Closing wav output resources")
        rawPcmInputStream.close()
        wavFile.close()

        Log.d(TAG, "Conversion finished.")
        return Ok(true)
    }

    private fun writeWavHeader(
        out: RandomAccessFile,
        channels: Short,
        bitDepth: Short,
        sampleRate: Int,
        input: InputStream
    ) {
        val byteRate = sampleRate * channels * (bitDepth / 8)
        val blockAlign = (channels * bitDepth / 8).toShort()

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk descriptor
            putInt(0x52494646) // "RIFF"
            putInt(0) // size is calculated after data is available
            putInt(0x57415645) // "WAVE"

            // fmt sub-chunk
            putInt(0x666d7420) // "fmt "
            putInt(16) // size of fmt chunk
            putShort(1) // format = 1 for PCM
            putShort(channels)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitDepth)

            // data sub-chunk
            putInt(0x64617461) // "data"
            putInt(0) // size is calculated after data is available
        }

        out.write(header.array())

        // Write PCM data
        val buffer = ByteArray(4096)
        var bytesRead = input.read(buffer)
        var totalBytes = 0
        while (bytesRead >= 0) {
            out.write(buffer, 0, bytesRead)
            totalBytes += bytesRead
            bytesRead = input.read(buffer)
        }

        // Go back and update chunk sizes
        val fileSize = totalBytes + 36 // 36 is the size of the rest of the header
        val dataSize = totalBytes
        val fileSizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileSize)
        val dataSizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize)

        // It's important to use RandomAccessFile here to be able to go back and write the size in the header.
        out.seek(4)
        out.write(fileSizeBuffer.array())
        out.seek(40)
        out.write(dataSizeBuffer.array())
    }
}

data class TrackWithFormat(val index: Int, val format: MediaFormat)

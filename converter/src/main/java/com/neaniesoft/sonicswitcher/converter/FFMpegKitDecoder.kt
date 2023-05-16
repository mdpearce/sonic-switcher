package com.neaniesoft.sonicswitcher.converter

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class FFMpegKitDecoder(private val context: Context) : AudioFileConverter {
    companion object {
        private const val TAG = "FFMpegKitDecoder"
    }

    override suspend fun convertAudioFile(input: Uri, output: Uri): Result<Boolean, ConverterError> {
        val inputPath = FFmpegKitConfig.getSafParameterForRead(context, input)
        val outputPath = FFmpegKitConfig.getSafParameterForWrite(context, output)

        Log.d(TAG, "Starting FFMpeg session")

        val session = FFmpegKit.execute("-i $inputPath -acodec libmp3lame -b:a 256k $outputPath")

        Log.d(TAG, "FFMpeg session complete")
        return when {
            ReturnCode.isSuccess(session.returnCode) -> {
                Log.d(TAG, "Success")
                Ok(true)
            }

            ReturnCode.isCancel(session.returnCode) -> {
                Log.d(TAG, "Cancel")
                Ok(false)
            }

            else -> {
                Log.d(
                    TAG,
                    String.format(
                        "Command failed with state %s and rc %s.%s",
                        session.getState(),
                        session.getReturnCode(),
                        session.getFailStackTrace()
                    )
                )
                Err(FFMpegError)
            }
        }
    }
}

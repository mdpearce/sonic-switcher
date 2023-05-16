package com.neaniesoft.sonicswitcher.converter

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FFMpegKitDecoder(private val context: Context) : AudioFileConverter {
    companion object {
        private const val TAG = "FFMpegKitDecoder"
    }

    override suspend fun convertAudioFile(input: Uri, output: Uri): ConversionResult =
        suspendCancellableCoroutine { cont ->
            val inputPath = FFmpegKitConfig.getSafParameterForRead(context, input)
            val outputPath = FFmpegKitConfig.getSafParameterForWrite(context, output)

            Log.d(TAG, "Starting FFMpeg session")

            val session = FFmpegKit.executeAsync(
                "-i $inputPath -acodec libmp3lame -b:a 256k $outputPath",
                { session ->
                    val returnCode = session.returnCode
                    if (returnCode.isValueCancel) {
                        cont.resume(ConversionCancelled)
                    } else if (returnCode.isValueError) {
                        cont.resumeWithException(ConversionException("Command failed with state ${session.state} and rc ${session.returnCode}"))
                    } else {
                        cont.resume(ConversionComplete)
                    }
                },
                { log ->
                    val logFn: (String) -> Unit = when (log.level) {
                        Level.AV_LOG_DEBUG -> {
                            { Log.d(TAG, it) }
                        }

                        Level.AV_LOG_STDERR,
                        Level.AV_LOG_FATAL,
                        Level.AV_LOG_PANIC,
                        Level.AV_LOG_ERROR -> {
                            { Log.e(TAG, it) }
                        }

                        Level.AV_LOG_QUIET -> {
                            {}
                        }

                        Level.AV_LOG_WARNING -> {
                            { Log.w(TAG, it) }
                        }

                        Level.AV_LOG_INFO -> {
                            { Log.i(TAG, it) }
                        }

                        Level.AV_LOG_VERBOSE -> {
                            { Log.v(TAG, it) }
                        }

                        Level.AV_LOG_TRACE -> {
                            { Log.v(TAG, it) }
                        }
                    }
                    logFn(log.message)
                },
                { statistics -> Log.d(TAG, "Statistics update: ${statistics.time}") }
            )
            cont.invokeOnCancellation { session.cancel() }
        }
}

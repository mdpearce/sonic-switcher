package com.neaniesoft.sonicswitcher.converter

import android.net.Uri

interface AudioFileConverter {
    suspend fun convertAudioFile(input: Uri, output: Uri, onProgressUpdated: (ProgressUpdate) -> Unit): ConversionResult
}

sealed class ConversionResult
object ConversionCancelled : ConversionResult()
object ConversionComplete : ConversionResult()

data class ConversionException(override val message: String) : Throwable(message)

sealed class ProgressUpdate

object Inactive : ProgressUpdate()
data class Processing(val complete: Float) : ProgressUpdate()

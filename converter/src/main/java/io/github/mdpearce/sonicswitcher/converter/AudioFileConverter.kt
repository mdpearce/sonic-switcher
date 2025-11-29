package io.github.mdpearce.sonicswitcher.converter

import android.net.Uri
import io.github.mdpearce.sonicswitcher.converter.results.ConversionResult
import io.github.mdpearce.sonicswitcher.converter.results.ProgressUpdate

interface AudioFileConverter {
    suspend fun convertAudioFile(
        input: Uri,
        output: Uri,
        onProgressUpdated: (ProgressUpdate) -> Unit,
    ): ConversionResult
}

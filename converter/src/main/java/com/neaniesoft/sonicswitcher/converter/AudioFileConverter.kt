package com.neaniesoft.sonicswitcher.converter

import android.net.Uri
import com.neaniesoft.sonicswitcher.converter.results.ConversionResult
import com.neaniesoft.sonicswitcher.converter.results.ProgressUpdate

interface AudioFileConverter {
    suspend fun convertAudioFile(input: Uri, output: Uri, onProgressUpdated: (ProgressUpdate) -> Unit): ConversionResult
}

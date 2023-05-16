package com.neaniesoft.sonicswitcher.converter

import android.net.Uri
import com.github.michaelbull.result.Result

interface AudioFileConverter {
    suspend fun convertAudioFile(input: Uri, output: Uri): Result<Boolean, ConverterError>
}

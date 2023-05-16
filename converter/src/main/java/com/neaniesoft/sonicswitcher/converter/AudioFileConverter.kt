package com.neaniesoft.sonicswitcher.converter

import android.net.Uri
import com.github.michaelbull.result.Result

interface AudioFileConverter {
    suspend fun decodeToPcm(input: Uri, output: Uri): Result<Boolean, ConverterError>
}

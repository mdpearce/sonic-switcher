package com.neaniesoft.sonicswitcher.converter

import android.net.Uri
import com.github.michaelbull.result.Result

interface PcmDecoder {
    suspend fun decodeToPcm(input: Uri, outputPath: Uri): Result<Boolean, ConverterError>
}

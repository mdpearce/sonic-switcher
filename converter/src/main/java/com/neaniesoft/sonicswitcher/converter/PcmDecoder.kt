package com.neaniesoft.sonicswitcher.converter

import android.net.Uri
import com.github.michaelbull.result.Result
import java.io.File

interface PcmDecoder {
    fun decodeToPcm(input: Uri, outputPath: String): Result<File, ConverterError>
}

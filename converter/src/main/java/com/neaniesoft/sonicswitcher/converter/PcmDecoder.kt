package com.neaniesoft.sonicswitcher.converter

import com.github.michaelbull.result.Result
import java.io.File

interface PcmDecoder {
    fun decodeToPcm(inputFile: File, outputPath: String): Result<File, ConverterError>
}

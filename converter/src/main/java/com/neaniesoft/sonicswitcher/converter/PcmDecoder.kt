package com.neaniesoft.sonicswitcher.converter

import com.github.michaelbull.result.Result
import java.io.File
import java.io.FileDescriptor

interface PcmDecoder {
    fun decodeToPcm(inputFile: FileDescriptor, outputPath: String): Result<File, ConverterError>
}

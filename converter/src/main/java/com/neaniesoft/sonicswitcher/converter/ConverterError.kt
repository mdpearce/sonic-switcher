package com.neaniesoft.sonicswitcher.converter

sealed class ConverterError

object NoAudioTrackError : ConverterError()

object NoDecoderError : ConverterError()

object NoInputBufferError : ConverterError()

object NoOutputBufferError : ConverterError()

data class OutputFileError(val throwable: Throwable) : ConverterError()

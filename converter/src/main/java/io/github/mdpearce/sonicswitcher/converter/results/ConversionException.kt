package io.github.mdpearce.sonicswitcher.converter.results

data class ConversionException(override val message: String) : Throwable(message)

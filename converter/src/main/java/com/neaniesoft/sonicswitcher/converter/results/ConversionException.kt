package com.neaniesoft.sonicswitcher.converter.results

data class ConversionException(override val message: String) : Throwable(message)

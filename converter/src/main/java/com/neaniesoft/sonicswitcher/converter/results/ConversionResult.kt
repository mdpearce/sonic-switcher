package com.neaniesoft.sonicswitcher.converter.results

sealed class ConversionResult
object ConversionCancelled : ConversionResult()
object ConversionComplete : ConversionResult()

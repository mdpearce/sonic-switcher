package io.github.mdpearce.sonicswitcher.converter.results

sealed class ProgressUpdate

object Inactive : ProgressUpdate()

data class Processing(
    val complete: Float,
) : ProgressUpdate()

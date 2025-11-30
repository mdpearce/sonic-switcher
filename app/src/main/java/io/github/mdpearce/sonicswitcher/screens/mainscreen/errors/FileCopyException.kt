package io.github.mdpearce.sonicswitcher.screens.mainscreen.errors

class FileCopyException(
    message: String,
    cause: Throwable?,
) : Exception(message, cause)

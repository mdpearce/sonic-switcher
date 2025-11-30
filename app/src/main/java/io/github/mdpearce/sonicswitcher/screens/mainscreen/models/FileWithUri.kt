package io.github.mdpearce.sonicswitcher.screens.mainscreen.models

import android.net.Uri
import java.io.File

data class FileWithUri(
    val file: File,
    val uri: Uri,
)

package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri

sealed class UiEvents
object OpenFileChooser : UiEvents()
object OpenOutputFileChooser : UiEvents()
data class OpenShareSheet(val uri: Uri) : UiEvents()

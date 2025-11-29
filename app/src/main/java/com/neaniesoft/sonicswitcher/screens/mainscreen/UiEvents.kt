package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri

sealed class UiEvents

object OpenFileChooser : UiEvents()

data class OpenOutputFileChooser(val defaultFilename: String) : UiEvents()

data class OpenShareSheet(val uri: Uri) : UiEvents()

data class OpenShareSheetForMultiple(val uris: List<Uri>) : UiEvents()

package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri
import com.neaniesoft.sonicswitcher.converter.results.ProgressUpdate

sealed class ScreenState

object Empty : ScreenState()

data class InputFileChosen(val inputFile: Uri, val inputDisplayName: String) : ScreenState()

data class Processing(val progressUpdate: ProgressUpdate) : ScreenState()

data class Complete(val outputFile: Uri) : ScreenState()

data class Error(val message: String) : ScreenState()

package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neaniesoft.sonicswitcher.converter.AudioFileConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val pcmDecoder: AudioFileConverter
) : ViewModel() {

    private val _uiEvents: MutableSharedFlow<UiEvents> = MutableSharedFlow()
    val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _inputFileDetails: MutableStateFlow<String> = MutableStateFlow("")
    val inputFileDetails: StateFlow<String> = _inputFileDetails.asStateFlow()

    private var inputFile: Uri? = null
        set(value) {
            field = value
            _inputFileDetails.value = value.toString()
        }

    fun onConvertClicked() {
        if (inputFile != null) { // This is gross - pass the value around instead, dumbass.
            viewModelScope.launch { _uiEvents.emit(OpenOutputFileChooser) }
        }
    }

    fun onOpenFileChooserClicked() {
        viewModelScope.launch { _uiEvents.emit(OpenFileChooser) }
    }

    fun onInputFileChosen(uri: Uri?) {
        inputFile = uri
    }

    fun onOutputPathChosen(outputPath: Uri?) {
        val input = inputFile

        if (input != null && outputPath != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = pcmDecoder.decodeToPcm(input, outputPath)
                Log.d("MainScreenViewModel", "result: $result")
            }
        }
    }
}

sealed class UiEvents
object OpenFileChooser : UiEvents()
object OpenOutputFileChooser : UiEvents()

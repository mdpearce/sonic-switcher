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
    private val audioFileConverter: AudioFileConverter
) : ViewModel() {

    private val _uiEvents: MutableSharedFlow<UiEvents> = MutableSharedFlow()
    val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _inputFile: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val inputFile: StateFlow<Uri> = _inputFile.asStateFlow()

    private val _isConverting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    fun onConvertClicked(inputUri: Uri) {
        if (inputUri != Uri.EMPTY) {
            viewModelScope.launch { _uiEvents.emit(OpenOutputFileChooser) }
        }
    }

    fun onOpenFileChooserClicked() {
        viewModelScope.launch { _uiEvents.emit(OpenFileChooser) }
    }

    fun onInputFileChosen(uri: Uri?) {
        _inputFile.value = uri ?: Uri.EMPTY
    }

    fun onOutputPathChosen(inputUri: Uri, outputUri: Uri) {
        if (inputUri != Uri.EMPTY && outputUri != Uri.EMPTY) {
            viewModelScope.launch(Dispatchers.IO) {
                _isConverting.value = true
                val result = audioFileConverter.convertAudioFile(inputUri, outputUri, {})
                Log.d("MainScreenViewModel", "result: $result")
                _isConverting.value = false
            }
        }
    }
}

sealed class UiEvents
object OpenFileChooser : UiEvents()
object OpenOutputFileChooser : UiEvents()

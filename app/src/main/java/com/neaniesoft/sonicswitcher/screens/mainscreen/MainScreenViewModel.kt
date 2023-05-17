package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neaniesoft.sonicswitcher.converter.AudioFileConverter
import com.neaniesoft.sonicswitcher.converter.Inactive
import com.neaniesoft.sonicswitcher.converter.ProgressUpdate
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
    private val audioFileConverter: AudioFileConverter,
    private val getFileDisplayName: GetFileDisplayNameUseCase
) : ViewModel() {

    private val _uiEvents: MutableSharedFlow<UiEvents> = MutableSharedFlow()
    val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _inputFile: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val inputFile: StateFlow<Uri> = _inputFile.asStateFlow()

    private val _progress: MutableStateFlow<ProgressUpdate> = MutableStateFlow(Inactive)
    val progress: StateFlow<ProgressUpdate> = _progress.asStateFlow()

    private val _inputDisplayName: MutableStateFlow<String> = MutableStateFlow("")
    val inputDisplayName = _inputDisplayName.asStateFlow()

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
        _inputDisplayName.value = uri?.let { getFileDisplayName(it) } ?: ""
    }

    fun onOutputPathChosen(inputUri: Uri, outputUri: Uri) {
        if (inputUri != Uri.EMPTY && outputUri != Uri.EMPTY) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = audioFileConverter.convertAudioFile(inputUri, outputUri) { progress ->
                    _progress.value = progress
                }
                Log.d("MainScreenViewModel", "result: $result")
            }
        }
    }
}

sealed class UiEvents
object OpenFileChooser : UiEvents()
object OpenOutputFileChooser : UiEvents()

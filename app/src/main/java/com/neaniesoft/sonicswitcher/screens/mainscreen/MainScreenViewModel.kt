package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neaniesoft.sonicswitcher.converter.PcmDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val pcmDecoder: PcmDecoder
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
        val uri = inputFile
        if (uri != null) {
            pcmDecoder.decodeToPcm(
                uri,
                Environment.getExternalStorageDirectory().path + "output.pcm"
            )
        }
    }

    fun onOpenFileChooserClicked() {
        viewModelScope.launch { _uiEvents.emit(OpenFileChooser) }
    }

    fun onInputFileChosen(uri: Uri?) {
        inputFile = uri
    }
}

sealed class UiEvents
object OpenFileChooser : UiEvents()

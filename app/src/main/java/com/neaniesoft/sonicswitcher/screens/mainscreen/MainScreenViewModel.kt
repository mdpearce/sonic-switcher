package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.content.ContentResolver
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val pcmDecoder: PcmDecoder,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _uiEvents: MutableSharedFlow<UiEvents> = MutableSharedFlow()
    val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _inputFileDetails: MutableStateFlow<String> = MutableStateFlow("")
    val inputFileDetails: StateFlow<String> = _inputFileDetails.asStateFlow()

    fun onConvertClicked() {
        pcmDecoder.decodeToPcm(
            File(Environment.getExternalStorageDirectory().path + "/sample.m4a"),
            Environment.getExternalStorageDirectory().path + "output.pcm"
        )
    }

    fun onOpenFileChooserClicked() {
        viewModelScope.launch { _uiEvents.emit(OpenFileChooser) }
    }

    fun onInputFileChosen(uri: Uri?) {
        if (uri != null) {
            viewModelScope.launch {
                _inputFileDetails.value = uri.toString()
            }
        }
    }
}

sealed class UiEvents
object OpenFileChooser : UiEvents()

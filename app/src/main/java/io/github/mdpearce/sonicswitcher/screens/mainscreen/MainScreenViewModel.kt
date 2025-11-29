package io.github.mdpearce.sonicswitcher.screens.mainscreen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mdpearce.sonicswitcher.converter.AudioFileConverter
import io.github.mdpearce.sonicswitcher.converter.results.ConversionCancelled
import io.github.mdpearce.sonicswitcher.converter.results.ConversionComplete
import io.github.mdpearce.sonicswitcher.converter.results.ConversionError
import io.github.mdpearce.sonicswitcher.converter.results.ConversionException
import io.github.mdpearce.sonicswitcher.converter.results.Inactive
import io.github.mdpearce.sonicswitcher.data.ConvertedFile
import io.github.mdpearce.sonicswitcher.data.ConvertedFileRepository
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.AddFileToQueueUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.BuildFilenameUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.ClearQueueUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.CopyInputFileToTempDirectoryUseCase
import io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases.GetFileDisplayNameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel
    @Inject
    constructor(
        private val audioFileConverter: AudioFileConverter,
        private val getFileDisplayName: GetFileDisplayNameUseCase,
        private val buildFilename: BuildFilenameUseCase,
        private val copyInputFileToTempDirectory: CopyInputFileToTempDirectoryUseCase,
        private val addFileToQueue: AddFileToQueueUseCase,
        private val clearQueue: ClearQueueUseCase,
        private val repository: ConvertedFileRepository,
    ) : ViewModel() {
        private val _uiEvents: MutableSharedFlow<UiEvents> = MutableSharedFlow()
        val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

        private val _screenState: MutableStateFlow<ScreenState> = MutableStateFlow(Empty)
        val screenState = _screenState.asStateFlow()

        val queuedFiles: StateFlow<List<ConvertedFile>> =
            repository
                .getAllFiles()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        val queueCount: StateFlow<Int> =
            repository
                .getFileCount()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0,
                )

        var sharedInputUri: Uri = Uri.EMPTY
            set(value) {
                if (field != value) {
                    field = value
                    onInputFileChosen(value)
                }
            }

        fun onOpenFileChooserClicked() {
            viewModelScope.launch { _uiEvents.emit(OpenFileChooser) }
        }

        fun onInputFileChosen(uri: Uri?) {
            val finalUri = uri ?: Uri.EMPTY
            _screenState.value =
                if (finalUri != Uri.EMPTY) {
                    InputFileChosen(finalUri, getFileDisplayName(finalUri))
                } else {
                    Empty
                }
        }

        fun onConvertClicked(inputUri: Uri) {
            if (inputUri != Uri.EMPTY) {
                viewModelScope.launch { _uiEvents.emit(OpenOutputFileChooser(buildFilename())) }
            }
        }

        fun onOutputFileChosen(
            inputUri: Uri,
            outputUri: Uri,
        ) {
            if (inputUri != Uri.EMPTY && outputUri != Uri.EMPTY) {
                _screenState.value = Processing(Inactive)
                viewModelScope.launch(Dispatchers.IO) {
                    val tempInputFile = copyInputFileToTempDirectory(inputUri)
                    val result =
                        try {
                            audioFileConverter.convertAudioFile(tempInputFile.uri, outputUri) { progress ->
                                _screenState.value = Processing(progress)
                            }
                        } catch (e: ConversionException) {
                            ConversionError(e)
                        }
                    Log.d("MainScreenViewModel", "result: $result")
                    _screenState.value =
                        when (result) {
                            is ConversionComplete -> Complete(outputUri)
                            is ConversionCancelled -> Error("Conversion cancelled.")
                            is ConversionError -> Error(result.throwable.message)
                        }
                    tempInputFile.file.delete()
                }
            }
        }

        fun onShareClicked(uri: Uri) {
            viewModelScope.launch {
                _uiEvents.emit(OpenShareSheet(uri))
            }
        }

        fun onAddToQueueClicked(uri: Uri) {
            viewModelScope.launch {
                addFileToQueue(uri)
                _screenState.value = Empty
            }
        }

        fun onShareAllQueuedClicked() {
            viewModelScope.launch {
                val files = queuedFiles.value
                if (files.isNotEmpty()) {
                    _uiEvents.emit(OpenShareSheetForMultiple(files.map { it.uri }))
                }
            }
        }

        fun onClearQueueClicked() {
            viewModelScope.launch {
                clearQueue()
            }
        }
    }

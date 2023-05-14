package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.os.Environment
import androidx.lifecycle.ViewModel
import com.neaniesoft.sonicswitcher.converter.PcmDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(private val pcmDecoder: PcmDecoder) : ViewModel() {

    fun onConvertClicked() {
        pcmDecoder.decodeToPcm(
            File(Environment.getExternalStorageDirectory().path + "/sample.m4a"),
            Environment.getExternalStorageDirectory().path + "output.pcm"
        )
    }
}

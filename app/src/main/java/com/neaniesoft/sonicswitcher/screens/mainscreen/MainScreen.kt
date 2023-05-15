package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.media.MediaExtractor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neaniesoft.sonicswitcher.converter.PcmDecoderImpl

@Composable
fun MainScreen(viewModel: MainScreenViewModel = viewModel()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Button(onClick = {}) {
                Text(text = "Choose file")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.onConvertClicked() }
            ) {
                Text(text = "Convert")
            }
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() = MainScreen(MainScreenViewModel(PcmDecoderImpl { MediaExtractor() }))

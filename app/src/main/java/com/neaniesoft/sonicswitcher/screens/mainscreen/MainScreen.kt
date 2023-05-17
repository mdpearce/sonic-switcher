package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neaniesoft.sonicswitcher.converter.Inactive
import com.neaniesoft.sonicswitcher.converter.Processing
import com.neaniesoft.sonicswitcher.converter.ProgressUpdate

@Composable
fun MainScreen(viewModel: MainScreenViewModel = viewModel()) {
    val inputFileUri by viewModel.inputFile.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val inputDisplayName by viewModel.inputDisplayName.collectAsState()

    val inputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onInputFileChosen(result.data?.data)
        }
    val outputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onOutputPathChosen(inputFileUri, result.data?.data ?: Uri.EMPTY)
        }

    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is OpenFileChooser -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    inputFileChooser.launch(intent)
                }

                is OpenOutputFileChooser -> {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/mpeg"
                        putExtra(Intent.EXTRA_TITLE, "converted.mp3")
                    }
                    outputFileChooser.launch(intent)
                }
            }
        }
    }

    MainScreenContent(
        onOpenFileChooserClicked = viewModel::onOpenFileChooserClicked,
        onConvertClicked = { viewModel.onConvertClicked(inputFileUri) },
        progress = progress,
        inputDisplayName = inputDisplayName
    )
}

@Composable
fun MainScreenContent(
    onOpenFileChooserClicked: () -> Unit,
    onConvertClicked: () -> Unit,
    progress: ProgressUpdate,
    inputDisplayName: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (progress is Processing) {
            CircularProgressIndicator(
                progress = progress.complete,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(text = inputDisplayName, modifier = Modifier.align(Alignment.Center))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Button(onClick = { onOpenFileChooserClicked() }) {
                Text(text = "Choose file")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { onConvertClicked() }
            ) {
                Text(text = "Convert")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() = MainScreenContent({}, {}, Inactive, "File.m4a")

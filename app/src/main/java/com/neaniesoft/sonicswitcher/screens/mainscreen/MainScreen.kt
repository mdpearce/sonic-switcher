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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neaniesoft.sonicswitcher.converter.results.Inactive
import com.neaniesoft.sonicswitcher.converter.results.Processing
import com.neaniesoft.sonicswitcher.converter.results.ProgressUpdate

@Composable
fun MainScreen(sharedUri: Uri, viewModel: MainScreenViewModel = viewModel()) {
    val inputFileUri by viewModel.inputFile.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val inputDisplayName by viewModel.inputDisplayName.collectAsState()
    val outputFileUri by viewModel.outputFile.collectAsState()

    val inputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onInputFileChosen(result.data?.data)
        }
    val outputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onOutputPathChosen(inputFileUri, result.data?.data ?: Uri.EMPTY)
        }

    val context = LocalContext.current

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

                is OpenShareSheet -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        type = "audio/mpeg"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.grantUriPermission(
                        context.packageName,
                        event.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    context.startActivity(intent)
                }
            }
        }
    }

    if (sharedUri != Uri.EMPTY) {
        viewModel.onInputFileChosen(sharedUri)
    }

    MainScreenContent(
        onOpenFileChooserClicked = viewModel::onOpenFileChooserClicked,
        onConvertClicked = { viewModel.onConvertClicked(inputFileUri) },
        onShareClicked = viewModel::onShareClicked,
        progress = progress,
        inputDisplayName = inputDisplayName,
        outputFile = outputFileUri
    )
}

@Composable
fun MainScreenContent(
    onOpenFileChooserClicked: () -> Unit,
    onConvertClicked: () -> Unit,
    onShareClicked: (Uri) -> Unit,
    progress: ProgressUpdate,
    inputDisplayName: String,
    outputFile: Uri
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            progress is Processing -> {
                CircularProgressIndicator(
                    progress = progress.complete,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            outputFile != Uri.EMPTY -> {
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = { onShareClicked(outputFile) }
                ) {
                    Text(text = "Share file")
                }
            }

            else -> {
                Text(text = inputDisplayName, modifier = Modifier.align(Alignment.Center))
            }
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
fun PreviewMainScreen() = MainScreenContent({}, {}, {}, Inactive, "File.m4a", Uri.EMPTY)

package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neaniesoft.sonicswitcher.R
import com.neaniesoft.sonicswitcher.converter.results.Inactive
import com.neaniesoft.sonicswitcher.converter.results.Processing
import com.neaniesoft.sonicswitcher.ui.theme.AppTheme

@Composable
fun MainScreen(
    sharedUri: Uri,
    viewModel: MainScreenViewModel = viewModel(),
) {
    Log.d("MainScreen", "sharedUri: $sharedUri")
    val screenState by viewModel.screenState.collectAsState()

    val inputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onInputFileChosen(result.data?.data)
        }

    val outputFileChooser =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onOutputFileChosen(
                screenState.let { state ->
                    if (state is InputFileChosen) {
                        state.inputFile
                    } else {
                        Uri.EMPTY
                    }
                },
                result.data?.data ?: Uri.EMPTY,
            )
        }

    val context = LocalContext.current
    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is OpenFileChooser -> {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "audio/*"
                        }
                    inputFileChooser.launch(intent)
                }

                is OpenOutputFileChooser -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "audio/mpeg"
                            putExtra(Intent.EXTRA_TITLE, event.defaultFilename)
                        }
                    outputFileChooser.launch(intent)
                }

                is OpenShareSheet -> {
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, event.uri)
                            type = "audio/mpeg"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    context.grantUriPermission(
                        context.packageName,
                        event.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    context.startActivity(intent)
                }
            }
        }
    }

    if (sharedUri != Uri.EMPTY) {
        viewModel.sharedInputUri = sharedUri
    }

    MainScreenContent(
        onOpenFileChooserClicked = viewModel::onOpenFileChooserClicked,
        onConvertClicked = viewModel::onConvertClicked,
        onShareClicked = viewModel::onShareClicked,
        screenState = screenState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    onOpenFileChooserClicked: () -> Unit,
    onConvertClicked: (Uri) -> Unit,
    onShareClicked: (Uri) -> Unit,
    screenState: ScreenState,
) {
    Log.d("MainScreenContent", "screenState: $screenState")
    Scaffold(topBar = {
        TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
    }) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (screenState) {
                is InputFileChosen ->
                    Text(
                        text = screenState.inputDisplayName,
                        modifier = Modifier.align(Alignment.Center),
                    )

                is com.neaniesoft.sonicswitcher.screens.mainscreen.Processing -> {
                    screenState.progressUpdate.let { update ->
                        when (update) {
                            is Inactive -> {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }

                            is Processing -> {
                                CircularProgressIndicator(
                                    progress = { update.complete },
                                    modifier =
                                        Modifier.align(
                                            Alignment.Center,
                                        ),
                                )
                            }
                        }
                    }
                }

                is Complete -> {
                    Button(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { onShareClicked(screenState.outputFile) },
                    ) {
                        Row {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(id = R.string.share_content_description),
                                Modifier.padding(end = 16.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.share_button),
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }

                is Error -> {
                    Text(
                        text = screenState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier.align(
                                Alignment.Center,
                            ),
                    )
                }

                Empty -> {
                    Text(
                        text = stringResource(id = R.string.select_a_file),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Button(onClick = { onOpenFileChooserClicked() }) {
                    Text(text = stringResource(id = R.string.choose_file_button))
                }
                Spacer(modifier = Modifier.weight(1f))
                screenState.let { state ->
                    if (state is InputFileChosen) {
                        Button(onClick = { onConvertClicked(state.inputFile) }) {
                            Text(text = stringResource(id = R.string.convert_button))
                        }
                    } else {
                        Button(onClick = {}, enabled = false) {
                            Text(text = stringResource(id = R.string.convert_button))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
fun PreviewMainScreen() =
    AppTheme {
        MainScreenContent({}, {}, {}, Empty)
    }

@Preview(showBackground = true, name = "Input file chosen")
@Composable
fun PreviewMainScreenInputFileChosen() =
    MainScreenContent(
        onOpenFileChooserClicked = { },
        onConvertClicked = {},
        onShareClicked = {},
        screenState = InputFileChosen(Uri.parse("http://some/url"), "Somefile.m4a"),
    )

@Preview(showBackground = true, name = "Processing Inactive")
@Composable
fun PreviewMainScreenProcessingInactive() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        screenState = Processing(Inactive),
    )

@Preview(showBackground = true, name = "Processing active")
@Composable
fun PreviewMainScreenProcessingActive() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        screenState = Processing(Processing(0.5f)),
    )

@Preview(showBackground = true, name = "Complete")
@Composable
fun PreviewMainScreenComplete() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        screenState = Complete(Uri.parse("http://some/url")),
    )

@Preview(showBackground = true, name = "Error")
@Composable
fun PreviewMainScreenError() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        screenState = Error("Something went wrong"),
    )

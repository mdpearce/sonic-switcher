package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.neaniesoft.sonicswitcher.data.ConvertedFile
import com.neaniesoft.sonicswitcher.ui.theme.AppTheme

@Composable
fun MainScreen(
    sharedUri: Uri,
    viewModel: MainScreenViewModel = viewModel(),
) {
    Log.d("MainScreen", "sharedUri: $sharedUri")
    val screenState by viewModel.screenState.collectAsState()
    val queuedFiles by viewModel.queuedFiles.collectAsState()
    val queueCount by viewModel.queueCount.collectAsState()

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
                    context.startActivity(Intent.createChooser(intent, null))
                }

                is OpenShareSheetForMultiple -> {
                    val intent =
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(event.uris))
                            type = "audio/mpeg"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    context.startActivity(Intent.createChooser(intent, null))
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
        onAddToQueueClicked = viewModel::onAddToQueueClicked,
        onShareAllQueuedClicked = viewModel::onShareAllQueuedClicked,
        onClearQueueClicked = viewModel::onClearQueueClicked,
        screenState = screenState,
        queuedFiles = queuedFiles,
        queueCount = queueCount,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    onOpenFileChooserClicked: () -> Unit,
    onConvertClicked: (Uri) -> Unit,
    onShareClicked: (Uri) -> Unit,
    onAddToQueueClicked: (Uri) -> Unit,
    onShareAllQueuedClicked: () -> Unit,
    onClearQueueClicked: () -> Unit,
    screenState: ScreenState,
    queuedFiles: List<ConvertedFile>,
    queueCount: Int,
) {
    Log.d("MainScreenContent", "screenState: $screenState")
    Scaffold(topBar = {
        TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
    }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Main content area
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
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
                            Button(
                                onClick = { onAddToQueueClicked(screenState.outputFile) },
                            ) {
                                Text(text = stringResource(id = R.string.add_to_queue_button))
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
            }

            // Queue section
            if (queueCount > 0) {
                HorizontalDivider()
                QueueSection(
                    queuedFiles = queuedFiles,
                    queueCount = queueCount,
                    onShareAllClicked = onShareAllQueuedClicked,
                    onClearQueueClicked = onClearQueueClicked,
                )
            }

            // Bottom buttons
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                    Modifier
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

@Composable
fun QueueSection(
    queuedFiles: List<ConvertedFile>,
    queueCount: Int,
    onShareAllClicked: () -> Unit,
    onClearQueueClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Queue header with actions
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.queue_header, queueCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onShareAllClicked) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(id = R.string.share_all_content_description),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(id = R.string.share_all_button, queueCount))
                }
                IconButton(onClick = onClearQueueClicked) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.clear_queue_content_description),
                    )
                }
            }
        }

        // Queue list
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(queuedFiles, key = { it.id }) { file ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = file.displayName,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
fun PreviewMainScreen() =
    AppTheme {
        MainScreenContent(
            onOpenFileChooserClicked = {},
            onConvertClicked = {},
            onShareClicked = {},
            onAddToQueueClicked = {},
            onShareAllQueuedClicked = {},
            onClearQueueClicked = {},
            screenState = Empty,
            queuedFiles = emptyList(),
            queueCount = 0,
        )
    }

@Preview(showBackground = true, name = "Input file chosen")
@Composable
fun PreviewMainScreenInputFileChosen() =
    MainScreenContent(
        onOpenFileChooserClicked = { },
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = InputFileChosen(Uri.parse("http://some/url"), "Somefile.m4a"),
        queuedFiles = emptyList(),
        queueCount = 0,
    )

@Preview(showBackground = true, name = "Processing Inactive")
@Composable
fun PreviewMainScreenProcessingInactive() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = Processing(Inactive),
        queuedFiles = emptyList(),
        queueCount = 0,
    )

@Preview(showBackground = true, name = "Processing active")
@Composable
fun PreviewMainScreenProcessingActive() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = Processing(Processing(0.5f)),
        queuedFiles = emptyList(),
        queueCount = 0,
    )

@Preview(showBackground = true, name = "Complete")
@Composable
fun PreviewMainScreenComplete() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = Complete(Uri.parse("http://some/url")),
        queuedFiles = emptyList(),
        queueCount = 0,
    )

@Preview(showBackground = true, name = "Error")
@Composable
fun PreviewMainScreenError() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = Error("Something went wrong"),
        queuedFiles = emptyList(),
        queueCount = 0,
    )

@Preview(showBackground = true, name = "With Queue")
@Composable
fun PreviewMainScreenWithQueue() =
    MainScreenContent(
        onOpenFileChooserClicked = {},
        onConvertClicked = {},
        onShareClicked = {},
        onAddToQueueClicked = {},
        onShareAllQueuedClicked = {},
        onClearQueueClicked = {},
        screenState = Empty,
        queuedFiles =
            listOf(
                ConvertedFile(
                    id = 1,
                    uri = Uri.parse("http://some/url1"),
                    displayName = "converted_file_1.mp3",
                    timestampMillis = 1704067200000L,
                ),
                ConvertedFile(
                    id = 2,
                    uri = Uri.parse("http://some/url2"),
                    displayName = "converted_file_2.mp3",
                    timestampMillis = 1704153600000L,
                ),
                ConvertedFile(
                    id = 3,
                    uri = Uri.parse("http://some/url3"),
                    displayName = "converted_file_3.mp3",
                    timestampMillis = 1704240000000L,
                ),
            ),
        queueCount = 3,
    )

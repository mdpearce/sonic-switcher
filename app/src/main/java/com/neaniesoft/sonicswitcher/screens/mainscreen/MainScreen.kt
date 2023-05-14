package com.neaniesoft.sonicswitcher.screens.mainscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neaniesoft.sonicswitcher.ui.utils.viewModel
import me.tatarka.inject.annotations.Inject

typealias MainScreen = @Composable () -> Unit

@Inject
@Composable
fun MainScreen(mainScreenViewModel: () -> MainScreenViewModel) {
    val viewModel = viewModel { mainScreenViewModel() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { }, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(text = "Convert")
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() = MainScreen() { MainScreenViewModel() }

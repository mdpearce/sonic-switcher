package io.github.mdpearce.sonicswitcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdpearce.sonicswitcher.screens.mainscreen.MainScreen
import io.github.mdpearce.sonicswitcher.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val sharedUri = remember { mutableStateOf(Uri.EMPTY) }
                Log.d("MainActivity", "sharedUri: ${sharedUri.value}")
                val intent: Intent? = intent
                if (intent?.action == Intent.ACTION_SEND) {
                    if (intent.type?.startsWith("audio/") == true) {
                        @Suppress("DEPRECATION")
                        val uri =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                                    ?: Uri.EMPTY
                            } else {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: Uri.EMPTY
                            }
                        sharedUri.value = uri
                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(sharedUri.value)
                }
            }
        }
    }
}

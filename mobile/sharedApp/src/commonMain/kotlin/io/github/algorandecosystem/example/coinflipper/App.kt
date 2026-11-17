package io.github.algorandecosystem.example.coinflipper

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.algorandecosystem.example.coinflipper.ui.navigation.AppNavigation
import io.github.algorandecosystem.example.coinflipper.ui.theme.AppTheme

/** Root composable shared by the Android and iOS apps. Koin must be started first (see initKoin). */
@Composable
fun App() =
    AppTheme {
        MaterialTheme {
            AppNavigation()
        }
    }

internal expect fun openUrl(url: String?)

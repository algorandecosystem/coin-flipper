package io.github.algorandecosystem.example.coinflipper.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Embedded web page: android.webkit.WebView on Android, WKWebView on iOS. */
@Composable
expect fun PlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
)

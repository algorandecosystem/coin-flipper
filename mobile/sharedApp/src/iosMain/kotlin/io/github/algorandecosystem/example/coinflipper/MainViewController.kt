package io.github.algorandecosystem.example.coinflipper

import androidx.compose.ui.window.ComposeUIViewController
import com.michaeltchuang.walletsdk.core.encryption.setKeychainAccessGroup
import io.github.algorandecosystem.example.coinflipper.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/** An App Group listed in `com.apple.security.application-groups` in iosApp/iosApp/iosApp.entitlements. */
private const val KEYCHAIN_ACCESS_GROUP = "group.io.github.algorandecosystem.example.coinflipper"

/** Entry point called from Swift (`MainViewControllerKt.MainViewController()`). */
@Suppress("ktlint:standard:function-naming", "FunctionName")
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        setKeychainAccessGroup(KEYCHAIN_ACCESS_GROUP)
        initKoin(platformContext = null)
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}

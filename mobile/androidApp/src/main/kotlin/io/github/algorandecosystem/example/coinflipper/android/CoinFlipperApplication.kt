package io.github.algorandecosystem.example.coinflipper.android

import android.app.Application
import io.github.algorandecosystem.example.coinflipper.di.initKoin

class CoinFlipperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Starts the wallet SDK (and Koin with this context), then loads the app's modules.
        initKoin(platformContext = this)
    }
}

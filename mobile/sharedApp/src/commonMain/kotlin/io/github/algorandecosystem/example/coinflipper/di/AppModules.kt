package io.github.algorandecosystem.example.coinflipper.di

import com.michaeltchuang.walletsdk.ui.initializeSdk.WalletSDK
import io.github.algorandecosystem.example.coinflipper.data.local.AppSettings
import io.github.algorandecosystem.example.coinflipper.domain.repositories.ActiveAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletLockRepository
import io.github.algorandecosystem.example.coinflipper.providePlatformModules
import org.koin.core.context.loadKoinModules
import org.koin.dsl.binds
import org.koin.dsl.module

fun appModules() =
    listOf(
        provideHttpClientModules,
        provideRepositoryModules,
        provideDomainModules,
        provideViewModelModules,
        providePlatformModules(), // Room DB & DataStore located here
        module {
            single { AppSettings(get()) } binds arrayOf(WalletLockRepository::class, ActiveAccountRepository::class)
        },
    )

/**
 * Starts the wallet SDK (which starts Koin with its own modules and, on Android, its context)
 * and then loads this app's modules into the same Koin instance.
 */
fun initKoin(platformContext: Any? = null) {
    WalletSDK.initialize(context = platformContext)
    loadKoinModules(appModules())
}

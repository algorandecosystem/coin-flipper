package io.github.algorandecosystem.example.coinflipper.di

import com.michaeltchuang.walletsdk.core.foundation.EventDelegate
import com.michaeltchuang.walletsdk.core.foundation.StateDelegate
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AccountViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AlgorandBaseViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.ValidatorDetailViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.ValidatorSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val provideViewModelModules =
    module {
        single { AlgorandBaseViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel {
            AccountViewModel(
                session = get(),
                refreshAccount = get(),
                lockWallet = get(),
                signOut = get(),
                stateDelegate = StateDelegate(),
                eventDelegate = EventDelegate(),
            )
        }
        viewModel {
            CoinFlipperViewModel(
                session = get(),
                refreshAccount = get(),
                getCurrentRound = get(),
                optIn = get(),
                closeOut = get(),
                placeBet = get(),
                settleBet = get(),
                stateDelegate = StateDelegate(),
                eventDelegate = EventDelegate(),
            )
        }
        single { ValidatorSearchViewModel(get()) }
        single { ValidatorDetailViewModel(get()) }
    }

package io.github.algorandecosystem.example.coinflipper.di

import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession
import io.github.algorandecosystem.example.coinflipper.domain.usecases.CloseOutOfCoinFlipperUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.CreateAccountUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.GetCurrentRoundUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.GetUnlockableAddressUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.LockWalletUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.OptInToCoinFlipperUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.PlaceBetUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.RefreshAccountUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.SettleBetUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.SignOutUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.UnlockWalletUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val provideDomainModules =
    module {
        single { AccountSession() }
        factoryOf(::RefreshAccountUseCase)
        factoryOf(::GetCurrentRoundUseCase)
        factoryOf(::OptInToCoinFlipperUseCase)
        factoryOf(::CloseOutOfCoinFlipperUseCase)
        factoryOf(::PlaceBetUseCase)
        factoryOf(::SettleBetUseCase)
        factoryOf(::LockWalletUseCase)
        factoryOf(::UnlockWalletUseCase)
        factoryOf(::GetUnlockableAddressUseCase)
        factoryOf(::SignOutUseCase)
        factoryOf(::CreateAccountUseCase)
    }

package io.github.algorandecosystem.example.coinflipper.di

import io.github.algorandecosystem.example.coinflipper.data.local.ExampleDatabase
import io.github.algorandecosystem.example.coinflipper.data.repositories.AlgorandRepository
import io.github.algorandecosystem.example.coinflipper.data.repositories.CoinFlipperRepositoryImpl
import io.github.algorandecosystem.example.coinflipper.data.repositories.ValidatorRepository
import io.github.algorandecosystem.example.coinflipper.data.repositories.WalletAccountRepositoryImpl
import io.github.algorandecosystem.example.coinflipper.domain.repositories.AccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository
import org.koin.dsl.module

val provideRepositoryModules =
    module {
        // Data-layer implementations bound to the domain interfaces the use cases depend on.
        single<AccountRepository> { AlgorandRepository(get()) }
        single<WalletAccountRepository> { WalletAccountRepositoryImpl(get(), get(), get(), get(), get()) }
        single<CoinFlipperRepository> { CoinFlipperRepositoryImpl(get()) }
        single { ValidatorRepository(get(), get<ExampleDatabase>().getDao(), get()) }
    }

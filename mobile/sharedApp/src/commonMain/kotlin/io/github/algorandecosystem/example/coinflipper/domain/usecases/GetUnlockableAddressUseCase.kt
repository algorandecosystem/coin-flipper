package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.ActiveAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository

/** The stored account [UnlockWalletUseCase] would sign in with, or null when there is none. */
class GetUnlockableAddressUseCase(
    private val walletAccounts: WalletAccountRepository,
    private val activeAccount: ActiveAccountRepository,
) {
    suspend operator fun invoke(): String? =
        walletAccounts.loadAccount(preferredAddress = activeAccount.getActiveAddress())?.address?.toString()
}

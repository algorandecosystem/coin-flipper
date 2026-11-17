package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository

/** Creates a new account without the wallet SDK's onboarding sheet and signs in with it. */
class CreateAccountUseCase(
    private val walletAccounts: WalletAccountRepository,
    private val unlockWallet: UnlockWalletUseCase,
) {
    suspend operator fun invoke(): Boolean {
        val existing = walletAccounts.getSignableAddresses()
        walletAccounts.createAccount()
        return unlockWallet(exclude = existing)
    }
}

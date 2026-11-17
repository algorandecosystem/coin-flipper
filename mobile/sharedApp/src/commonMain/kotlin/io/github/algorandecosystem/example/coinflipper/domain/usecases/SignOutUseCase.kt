package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.ActiveAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/**
 * Removes the signed-in account and its keys from the device, then signs out. Getting it back needs
 * its recovery phrase. Any other stored account stays locked until the user unlocks it.
 */
class SignOutUseCase(
    private val session: AccountSession,
    private val walletAccounts: WalletAccountRepository,
    private val activeAccount: ActiveAccountRepository,
    private val lockWallet: LockWalletUseCase,
) {
    suspend operator fun invoke() {
        session.account.value?.let { walletAccounts.deleteAccount(it.address.toString()) }
        activeAccount.setActiveAddress(null)
        lockWallet()
    }
}

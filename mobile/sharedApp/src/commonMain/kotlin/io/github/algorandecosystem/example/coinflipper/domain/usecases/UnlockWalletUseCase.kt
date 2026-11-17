package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.ActiveAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletLockRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/**
 * Signs in with an account stored in the wallet SDK, clears the lock and makes it the active account.
 * Prefers an account not in [exclude], so an account just added through onboarding replaces the
 * active one; otherwise returns to the last active account. Returns false if there is none.
 */
class UnlockWalletUseCase(
    private val session: AccountSession,
    private val walletAccounts: WalletAccountRepository,
    private val walletLock: WalletLockRepository,
    private val activeAccount: ActiveAccountRepository,
    private val refreshAccount: RefreshAccountUseCase,
) {
    suspend operator fun invoke(exclude: Set<String> = emptySet()): Boolean {
        val account = walletAccounts.loadAccount(exclude, activeAccount.getActiveAddress()) ?: return false
        walletLock.setLocked(false)
        activeAccount.setActiveAddress(account.address.toString())
        session.signIn(account)
        refreshAccount()
        return true
    }
}

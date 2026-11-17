package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletLockRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/**
 * Signs out and remembers that the wallet is locked. The account stays stored (encrypted) in the
 * wallet SDK, so it can be unlocked again without restoring it.
 */
class LockWalletUseCase(
    private val session: AccountSession,
    private val walletLock: WalletLockRepository,
) {
    suspend operator fun invoke() {
        walletLock.setLocked(true)
        session.signOut()
    }
}

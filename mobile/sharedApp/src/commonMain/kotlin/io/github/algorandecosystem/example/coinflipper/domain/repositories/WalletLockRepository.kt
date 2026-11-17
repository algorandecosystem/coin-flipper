package io.github.algorandecosystem.example.coinflipper.domain.repositories

/** Whether the user locked the wallet. Persisted so a locked wallet stays locked across launches. */
interface WalletLockRepository {
    suspend fun isLocked(): Boolean

    suspend fun setLocked(locked: Boolean)
}

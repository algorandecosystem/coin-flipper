package io.github.algorandecosystem.example.coinflipper.domain.repositories

/** The address of the account the user last signed in with, so Unlock returns to it. */
interface ActiveAccountRepository {
    suspend fun getActiveAddress(): String?

    suspend fun setActiveAddress(address: String?)
}

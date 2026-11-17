package io.github.algorandecosystem.example.coinflipper.domain.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount

/** Accounts created or recovered through the wallet SDK, which keeps their keys encrypted on device. */
interface WalletAccountRepository {
    /** Addresses of the stored accounts this app can sign with. */
    suspend fun getSignableAddresses(): Set<String>

    /**
     * Returns a signable account, preferring one whose address is not in [exclude] (so a freshly
     * created account wins over ones that already existed), then [preferredAddress]. Null when there is none.
     */
    suspend fun loadAccount(
        exclude: Set<String> = emptySet(),
        preferredAddress: String? = null,
    ): AlgorandAccount?

    /** Creates a Falcon25 account and stores its keys encrypted in the wallet SDK. Returns its address. */
    suspend fun createAccount(): String

    /** Permanently removes the account and its keys from the device. */
    suspend fun deleteAccount(address: String)
}

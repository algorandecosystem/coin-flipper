package io.github.algorandecosystem.example.coinflipper.domain.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount

/** Reads account and chain state from the Algorand network. */
interface AccountRepository {
    /** The account's balance and app local state, or null if algod can't be reached. */
    suspend fun getAccountInfo(account: AlgorandAccount): AccountInformation?

    /** The latest round algod has seen, or 0 if it can't be reached. */
    suspend fun getCurrentRound(): Long
}

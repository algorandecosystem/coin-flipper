package io.github.algorandecosystem.example.coinflipper.domain.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.domain.model.BetResult

/** Transactions against the coin flipper smart contract. Each call signs with [AlgorandAccount]. */
interface CoinFlipperRepository {
    /** Opts in to the app. Returns true once the transaction is confirmed. */
    suspend fun optIn(account: AlgorandAccount): Boolean

    /** Closes out of the app. Returns true once the transaction is confirmed. */
    suspend fun closeOut(account: AlgorandAccount): Boolean

    /** Places a bet and returns its commitment round, or null on failure. */
    suspend fun placeBet(
        account: AlgorandAccount,
        amountMicroAlgos: Long,
        isHeads: Boolean,
    ): Long?

    /** Settles the account's bet against the randomness beacon, or returns null on failure. */
    suspend fun settleBet(account: AlgorandAccount): BetResult?
}

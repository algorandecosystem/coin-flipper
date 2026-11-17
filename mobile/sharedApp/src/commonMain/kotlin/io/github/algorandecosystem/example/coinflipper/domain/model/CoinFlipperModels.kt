package io.github.algorandecosystem.example.coinflipper.domain.model

import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.utils.Constants

/** Outcome returned by the contract's `settle` method. */
data class BetResult(
    val won: Boolean,
    val amountMicroAlgos: Long,
)

/** The account's relationship with the coin flipper app, read from its local state. */
sealed interface CoinFlipperStatus {
    data object NotOptedIn : CoinFlipperStatus

    data object NoBet : CoinFlipperStatus

    /** A bet is waiting on the randomness beacon; it can be settled from [settleRound] onwards. */
    data class ActiveBet(
        val commitmentRound: Long,
    ) : CoinFlipperStatus {
        val settleRound: Long get() = commitmentRound + SETTLE_DELAY_ROUNDS
    }

    companion object {
        /** Rounds the beacon needs after the commitment round before a bet can be settled. */
        const val SETTLE_DELAY_ROUNDS = 10L
    }
}

fun AccountInformation.coinFlipperStatus(appId: Long = Constants.COINFLIP_APP_ID_TESTNET): CoinFlipperStatus {
    val localState = appsLocalState.firstOrNull { it.id == appId } ?: return CoinFlipperStatus.NotOptedIn
    val commitmentRound =
        localState.keyValue
            .firstOrNull { it.key == Constants.COINFLIP_APP_COMMITTMENT_ROUND_KEY }
            ?.value
            ?.uint
    return if (commitmentRound != null && commitmentRound > 0) {
        CoinFlipperStatus.ActiveBet(commitmentRound)
    } else {
        CoinFlipperStatus.NoBet
    }
}

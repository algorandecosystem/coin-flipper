package io.github.algorandecosystem.example.coinflipper.domain

import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.algorand.ApplicationLocalState
import io.github.algorandecosystem.example.coinflipper.algorand.TealKeyValue
import io.github.algorandecosystem.example.coinflipper.algorand.TealValue
import io.github.algorandecosystem.example.coinflipper.domain.model.CoinFlipperStatus
import io.github.algorandecosystem.example.coinflipper.domain.model.coinFlipperStatus
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel.Game
import io.github.algorandecosystem.example.coinflipper.ui.widgets.formatAlgos
import io.github.algorandecosystem.example.coinflipper.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals

class CoinFlipperModelsTest {
    private fun account(vararg localState: ApplicationLocalState) =
        AccountInformation(address = "A", amount = 1_000_000, appsLocalState = localState.toList())

    private fun commitment(round: Long) = TealKeyValue(Constants.COINFLIP_APP_COMMITTMENT_ROUND_KEY, TealValue(type = 2, uint = round))

    @Test
    fun notOptedInWithoutLocalState() {
        assertEquals(CoinFlipperStatus.NotOptedIn, account().coinFlipperStatus())
        assertEquals(CoinFlipperStatus.NotOptedIn, account(ApplicationLocalState(id = 1)).coinFlipperStatus())
    }

    @Test
    fun noBetWhenOptedInWithoutCommitment() {
        assertEquals(CoinFlipperStatus.NoBet, account(ApplicationLocalState(Constants.COINFLIP_APP_ID_TESTNET)).coinFlipperStatus())
    }

    @Test
    fun activeBetSettlesTenRoundsAfterCommitment() {
        val status = account(ApplicationLocalState(Constants.COINFLIP_APP_ID_TESTNET, listOf(commitment(100)))).coinFlipperStatus()
        assertEquals(CoinFlipperStatus.ActiveBet(100), status)
        assertEquals(110, (status as CoinFlipperStatus.ActiveBet).settleRound)
    }

    @Test
    fun waitingProgress() {
        assertEquals(0f, Game.WaitingForBeacon(currentRound = null, settleRound = 110).progress)
        assertEquals(0f, Game.WaitingForBeacon(currentRound = 90, settleRound = 110).progress)
        assertEquals(0.5f, Game.WaitingForBeacon(currentRound = 105, settleRound = 110).progress)
        assertEquals(1f, Game.WaitingForBeacon(currentRound = 120, settleRound = 110).progress)
    }

    @Test
    fun formatsMicroAlgos() {
        assertEquals("0 ALGO", formatAlgos(0))
        assertEquals("0.05 ALGO", formatAlgos(50_000))
        assertEquals("9.938 ALGO", formatAlgos(9_938_000))
        assertEquals("10 ALGO", formatAlgos(10_000_000))
    }
}

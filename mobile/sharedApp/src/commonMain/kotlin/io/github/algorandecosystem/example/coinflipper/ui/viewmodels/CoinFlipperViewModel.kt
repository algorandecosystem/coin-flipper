package io.github.algorandecosystem.example.coinflipper.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltchuang.walletsdk.core.foundation.EventDelegate
import com.michaeltchuang.walletsdk.core.foundation.EventViewModel
import com.michaeltchuang.walletsdk.core.foundation.StateDelegate
import com.michaeltchuang.walletsdk.core.foundation.StateViewModel
import io.github.algorandecosystem.example.coinflipper.domain.model.BetResult
import io.github.algorandecosystem.example.coinflipper.domain.model.CoinFlipperStatus
import io.github.algorandecosystem.example.coinflipper.domain.model.coinFlipperStatus
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession
import io.github.algorandecosystem.example.coinflipper.domain.usecases.CloseOutOfCoinFlipperUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.GetCurrentRoundUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.OptInToCoinFlipperUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.PlaceBetUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.RefreshAccountUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.SettleBetUseCase
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.generated.resources.error_account_flipcoin
import io.github.algorandecosystem.example.coinflipper.generated.resources.error_account_settle
import io.github.algorandecosystem.example.coinflipper.generated.resources.error_coinflipper_close_out
import io.github.algorandecosystem.example.coinflipper.generated.resources.error_coinflipper_opt_in
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_settle_wait
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_submit_bet
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_wait_message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** Coin Flipper tab: opt in/out, place a bet, wait for the randomness beacon, settle. */
class CoinFlipperViewModel(
    private val session: AccountSession,
    private val refreshAccount: RefreshAccountUseCase,
    private val getCurrentRound: GetCurrentRoundUseCase,
    private val optIn: OptInToCoinFlipperUseCase,
    private val closeOut: CloseOutOfCoinFlipperUseCase,
    private val placeBet: PlaceBetUseCase,
    private val settleBet: SettleBetUseCase,
    private val stateDelegate: StateDelegate<ViewState>,
    private val eventDelegate: EventDelegate<ViewEvent>,
) : ViewModel(),
    StateViewModel<CoinFlipperViewModel.ViewState> by stateDelegate,
    EventViewModel<CoinFlipperViewModel.ViewEvent> by eventDelegate {
    data class ViewState(
        val game: Game = Game.Loading,
        val balanceMicroAlgos: Long? = null,
        val isRefreshing: Boolean = false,
        /** True while a transaction is being signed and confirmed; actions are disabled. */
        val isSubmitting: Boolean = false,
    )

    sealed interface Game {
        data object Loading : Game

        data object NotOptedIn : Game

        data object PlaceBet : Game

        /** Bet placed; waiting for the beacon to reach [settleRound]. */
        data class WaitingForBeacon(
            val currentRound: Long?,
            val settleRound: Long,
        ) : Game {
            val progress: Float
                get() {
                    val round = currentRound ?: return 0f
                    val remaining = (settleRound - round).coerceIn(0, CoinFlipperStatus.SETTLE_DELAY_ROUNDS)
                    return 1f - remaining.toFloat() / CoinFlipperStatus.SETTLE_DELAY_ROUNDS
                }
        }

        data class ReadyToSettle(
            val currentRound: Long,
            val settleRound: Long,
        ) : Game
    }

    sealed interface ViewEvent {
        data class ShowMessage(
            val message: StringResource,
        ) : ViewEvent

        data class ShowBetResult(
            val result: BetResult,
        ) : ViewEvent
    }

    init {
        stateDelegate.setDefaultState(ViewState())
        observeAccount()
    }

    private fun observeAccount() {
        viewModelScope.launch {
            session.accountInfo.collect { info ->
                stateDelegate.updateState { it.copy(balanceMicroAlgos = info?.amount) }
            }
        }
        viewModelScope.launch {
            session.accountInfo
                .map { it?.coinFlipperStatus() }
                .distinctUntilChanged()
                .collectLatest { status ->
                    when (status) {
                        null -> setGame(Game.Loading)
                        CoinFlipperStatus.NotOptedIn -> setGame(Game.NotOptedIn)
                        CoinFlipperStatus.NoBet -> setGame(Game.PlaceBet)
                        // collectLatest cancels this loop when the status changes (e.g. after settling).
                        is CoinFlipperStatus.ActiveBet -> pollUntilSettleable(status.settleRound)
                    }
                }
        }
    }

    private suspend fun pollUntilSettleable(settleRound: Long) {
        setGame(Game.WaitingForBeacon(currentRound = null, settleRound = settleRound))
        while (true) {
            val round = getCurrentRound().takeIf { it > 0 }
            if (round != null && round >= settleRound) {
                setGame(Game.ReadyToSettle(round, settleRound))
                return
            }
            setGame(Game.WaitingForBeacon(round, settleRound))
            delay(ROUND_POLL_INTERVAL_MS)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            stateDelegate.updateState { it.copy(isRefreshing = true) }
            try {
                refreshAccount()
            } finally {
                stateDelegate.updateState { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onOptIn() =
        submit(Res.string.play_button_wait_message) {
            if (!optIn()) showMessage(Res.string.error_coinflipper_opt_in)
        }

    fun onCloseOut() =
        submit(Res.string.play_button_wait_message) {
            if (!closeOut()) showMessage(Res.string.error_coinflipper_close_out)
        }

    fun onPlaceBet(isHeads: Boolean) =
        submit(Res.string.play_button_submit_bet) {
            if (placeBet(isHeads) == null) showMessage(Res.string.error_account_flipcoin)
        }

    fun onSettle() =
        submit(Res.string.play_button_settle_wait) {
            val result = settleBet()
            if (result == null) {
                showMessage(Res.string.error_account_settle)
            } else {
                eventDelegate.sendEvent(ViewEvent.ShowBetResult(result))
            }
        }

    /** Runs one transaction at a time, then refreshes the account so the new state shows. */
    private fun submit(
        pendingMessage: StringResource,
        block: suspend () -> Unit,
    ) {
        if (stateDelegate.state.value.isSubmitting) return
        viewModelScope.launch {
            stateDelegate.updateState { it.copy(isSubmitting = true) }
            showMessage(pendingMessage)
            try {
                block()
                refreshAccount()
            } finally {
                stateDelegate.updateState { it.copy(isSubmitting = false) }
            }
        }
    }

    private suspend fun showMessage(message: StringResource) = eventDelegate.sendEvent(ViewEvent.ShowMessage(message))

    private fun setGame(game: Game) = stateDelegate.updateState { it.copy(game = game) }

    private companion object {
        /** Algorand rounds are ~2.8 s, so polling every 2 s keeps the progress current. */
        const val ROUND_POLL_INTERVAL_MS = 2_000L
    }
}

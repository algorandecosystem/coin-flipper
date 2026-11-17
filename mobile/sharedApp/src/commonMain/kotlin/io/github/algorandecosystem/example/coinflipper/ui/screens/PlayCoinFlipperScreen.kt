package io.github.algorandecosystem.example.coinflipper.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.generated.resources.coin_heads
import io.github.algorandecosystem.example.coinflipper.generated.resources.coin_tails
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_balance
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_close_out
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_opt_in
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_pending
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_button_settle
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_current_round
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_game_header
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_game_header2
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_game_instructions
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_result_lost
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_result_won
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_title
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_waiting_round
import io.github.algorandecosystem.example.coinflipper.ui.theme.md_theme_light_primary
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AlgorandBaseViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel.Game
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel.ViewEvent
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.CoinFlipperViewModel.ViewState
import io.github.algorandecosystem.example.coinflipper.ui.widgets.AlgorandButton
import io.github.algorandecosystem.example.coinflipper.ui.widgets.AlgorandDivider
import io.github.algorandecosystem.example.coinflipper.ui.widgets.RefreshableColumn
import io.github.algorandecosystem.example.coinflipper.ui.widgets.formatAlgos
import io.github.algorandecosystem.example.coinflipper.utils.Constants.DEFAULT_MICRO_ALGO_BET_AMOUNT
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Coin Flipper tab. Renders [CoinFlipperViewModel.ViewState] and forwards user intents; messages
 * arrive as one-shot [CoinFlipperViewModel.ViewEvent]s and go to the app's shared snackbar.
 */
@Composable
fun PlayCoinFlipperScreen(
    navController: NavController,
    algorandBaseViewModel: AlgorandBaseViewModel,
    tag: String,
    viewModel: CoinFlipperViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.viewEvent.collect { event ->
            val message =
                when (event) {
                    is ViewEvent.ShowMessage -> getString(event.message)
                    is ViewEvent.ShowBetResult -> {
                        val amount = formatAlgos(event.result.amountMicroAlgos)
                        getString(if (event.result.won) Res.string.play_result_won else Res.string.play_result_lost, amount)
                    }
                }
            algorandBaseViewModel.setSnackBarMessage(message)
        }
    }

    RefreshableColumn(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::onRefresh,
    ) {
        when (val game = state.game) {
            Game.Loading -> CircularProgressIndicator(color = md_theme_light_primary)
            Game.NotOptedIn ->
                AlgorandButton(stringResourceId = Res.string.play_button_opt_in, onClick = viewModel::onOptIn)
            else ->
                PlayingContent(
                    state = state,
                    game = game,
                    onPlaceBet = viewModel::onPlaceBet,
                    onSettle = viewModel::onSettle,
                    onCloseOut = viewModel::onCloseOut,
                )
        }
    }
}

@Composable
private fun PlayingContent(
    state: ViewState,
    game: Game,
    onPlaceBet: (isHeads: Boolean) -> Unit,
    onSettle: () -> Unit,
    onCloseOut: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.height(400.dp).fillMaxWidth(),
        ) {
            when (game) {
                Game.PlaceBet -> BetForm(state.balanceMicroAlgos, onPlaceBet)
                is Game.WaitingForBeacon -> {
                    RoundProgress(game.currentRound, game.settleRound, game.progress)
                    AlgorandButton(stringResourceId = Res.string.play_button_pending, onClick = {})
                }
                is Game.ReadyToSettle -> {
                    RoundProgress(game.currentRound, game.settleRound, 1f)
                    AlgorandButton(stringResourceId = Res.string.play_button_settle, onClick = onSettle)
                }
                Game.Loading, Game.NotOptedIn -> Unit
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.height(150.dp).fillMaxWidth(),
        ) {
            AlgorandDivider()
            AlgorandButton(stringResourceId = Res.string.play_button_close_out, onClick = onCloseOut)
        }
    }
}

@Composable
private fun BetForm(
    balanceMicroAlgos: Long?,
    onPlaceBet: (isHeads: Boolean) -> Unit,
) {
    GameText(stringResource(Res.string.play_title), fontSize = 24.sp, bold = true)
    balanceMicroAlgos?.let {
        GameText(stringResource(Res.string.play_balance) + " " + formatAlgos(it))
    }
    AlgorandDivider()
    GameText(
        stringResource(Res.string.play_game_header) +
            formatAlgos(DEFAULT_MICRO_ALGO_BET_AMOUNT.toLong()) +
            stringResource(Res.string.play_game_header2),
    )
    Row(
        modifier = Modifier.heightIn(max = 100.dp).height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.coin_heads),
            contentDescription = stringResource(Res.string.play_game_instructions),
            modifier = Modifier.fillMaxHeight().clickable { onPlaceBet(true) },
        )
        Image(
            painter = painterResource(Res.drawable.coin_tails),
            contentDescription = stringResource(Res.string.play_game_instructions),
            modifier = Modifier.fillMaxHeight().clickable { onPlaceBet(false) },
        )
    }
    AlgorandDivider()
    GameText(
        stringResource(Res.string.play_game_instructions),
        modifier = Modifier.padding(start = 30.dp, end = 30.dp),
    )
}

@Composable
private fun RoundProgress(
    currentRound: Long?,
    settleRound: Long,
    progress: Float,
) {
    currentRound?.let { GameText(stringResource(Res.string.play_current_round) + " $it", fontSize = 24.sp, bold = true) }
    GameText(stringResource(Res.string.play_waiting_round) + " $settleRound", fontSize = 24.sp, bold = true)
    CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.size(size = 100.dp),
        color = md_theme_light_primary,
        strokeWidth = 8.dp,
        trackColor = Color.Gray,
        strokeCap = StrokeCap.Round,
    )
    AlgorandDivider()
}

@Composable
private fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    bold: Boolean = false,
) {
    Text(
        text = text,
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = if (bold) FontWeight.Bold else null,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

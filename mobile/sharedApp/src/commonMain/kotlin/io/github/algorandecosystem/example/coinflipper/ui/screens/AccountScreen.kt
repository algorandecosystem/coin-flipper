package io.github.algorandecosystem.example.coinflipper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.michaeltchuang.walletsdk.ui.base.designsystem.theme.AlgoKitTheme
import com.michaeltchuang.walletsdk.ui.base.navigation.AlgoKitScreens
import com.michaeltchuang.walletsdk.ui.initializeSdk.WalletSDK
import com.michaeltchuang.walletsdk.ui.initializeSdk.rememberWalletSDKBottomSheetState
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_address
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_button_lock
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_button_manage
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_button_sign_out
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_sign_out_cancel
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_sign_out_confirm
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_sign_out_message
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_sign_out_title
import io.github.algorandecosystem.example.coinflipper.generated.resources.account_type
import io.github.algorandecosystem.example.coinflipper.generated.resources.play_balance
import io.github.algorandecosystem.example.coinflipper.ui.navigation.Screen
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AccountViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AccountViewModel.ViewEvent
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AccountViewModel.ViewState
import io.github.algorandecosystem.example.coinflipper.ui.widgets.AlgorandButton
import io.github.algorandecosystem.example.coinflipper.ui.widgets.PassphraseField
import io.github.algorandecosystem.example.coinflipper.ui.widgets.RefreshableColumn
import io.github.algorandecosystem.example.coinflipper.ui.widgets.formatAlgos
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    navController: NavController,
    tag: String,
    viewModel: AccountViewModel = koinViewModel(),
) {
    BackHandler {
        navController.popBackStack(Screen.HomeScreen.route, false)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val walletSheet = rememberWalletSDKBottomSheetState()

    LaunchedEffect(Unit) { viewModel.onRefresh() }
    LaunchedEffect(Unit) {
        viewModel.viewEvent.collect { event ->
            when (event) {
                is ViewEvent.OpenAccountDetails ->
                    walletSheet.show(initialScreen = AlgoKitScreens.ACCOUNT_STATUS_SCREEN, address = event.address)
            }
        }
    }

    when (val current = state) {
        // The tabs pop back to the login screen when there is no account.
        ViewState.NoAccount -> Unit
        is ViewState.Content ->
            AccountContent(
                state = current,
                onRefresh = viewModel::onRefresh,
                onAccountDetailsClick = viewModel::onAccountDetailsClick,
                onLockWallet = viewModel::onLockWallet,
                onSignOutClick = viewModel::onSignOutClick,
                onSignOutConfirm = viewModel::onSignOutConfirm,
                onSignOutDismiss = viewModel::onSignOutDismiss,
            )
    }

    AlgoKitTheme {
        WalletSDK.ShowAlgoKitWalletBottomSheet(
            state = walletSheet,
            onDismiss = viewModel::onAccountDetailsClosed,
            onAccountDeleted = viewModel::onAccountDeleted,
        )
    }
}

@Composable
private fun AccountContent(
    state: ViewState.Content,
    onRefresh: () -> Unit,
    onAccountDetailsClick: () -> Unit,
    onLockWallet: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutConfirm: () -> Unit,
    onSignOutDismiss: () -> Unit,
) {
    RefreshableColumn(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.background(Color.White),
    ) {
        PassphraseField(stringResource(Res.string.account_address), state.address)
        Text(
            text = stringResource(Res.string.account_type) + " " + state.accountType.displayName(),
            color = Color.Black,
            fontSize = 16.sp,
        )
        Text(
            text = stringResource(Res.string.play_balance) + " " + (state.balanceMicroAlgos?.let(::formatAlgos) ?: "…"),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
        // The passphrase is only shown inside the wallet SDK's guarded view-passphrase flow.
        AlgorandButton(stringResourceId = Res.string.account_button_manage, onClick = onAccountDetailsClick)
        AlgorandButton(stringResourceId = Res.string.account_button_lock, onClick = onLockWallet)
        AlgorandButton(stringResourceId = Res.string.account_button_sign_out, onClick = onSignOutClick)
    }
    if (state.isSignOutConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onSignOutDismiss,
            title = { Text(stringResource(Res.string.account_sign_out_title)) },
            text = { Text(stringResource(Res.string.account_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = onSignOutConfirm) {
                    Text(stringResource(Res.string.account_sign_out_confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = onSignOutDismiss) { Text(stringResource(Res.string.account_sign_out_cancel)) }
            },
        )
    }
}

private fun AlgorandAccount.Type.displayName(): String =
    when (this) {
        AlgorandAccount.Type.Falcon25 -> "Falcon25"
        AlgorandAccount.Type.Algo25 -> "Algo25"
        AlgorandAccount.Type.HdKey -> "HD Key"
    }

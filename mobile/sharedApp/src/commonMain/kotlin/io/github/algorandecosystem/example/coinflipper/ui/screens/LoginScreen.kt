package io.github.algorandecosystem.example.coinflipper.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.michaeltchuang.walletsdk.ui.base.designsystem.theme.AlgoKitTheme
import com.michaeltchuang.walletsdk.ui.base.navigation.AlgoKitScreens
import com.michaeltchuang.walletsdk.ui.initializeSdk.WalletSDK
import com.michaeltchuang.walletsdk.ui.initializeSdk.rememberWalletSDKBottomSheetState
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.generated.resources.coin_heads
import io.github.algorandecosystem.example.coinflipper.generated.resources.login_button_create
import io.github.algorandecosystem.example.coinflipper.generated.resources.login_button_restore
import io.github.algorandecosystem.example.coinflipper.generated.resources.login_button_unlock
import io.github.algorandecosystem.example.coinflipper.generated.resources.login_disclaimer
import io.github.algorandecosystem.example.coinflipper.generated.resources.login_guest_message
import io.github.algorandecosystem.example.coinflipper.ui.navigation.AlgorandExperienceTabs
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AlgorandBaseViewModel
import io.github.algorandecosystem.example.coinflipper.ui.widgets.AlgorandButton
import io.github.algorandecosystem.example.coinflipper.ui.widgets.AlgorandDivider
import io.github.algorandecosystem.example.coinflipper.utils.truncatedAlgorandAddress
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    navController: NavController,
    algorandBaseViewModel: AlgorandBaseViewModel,
    tag: String,
) {
    val account by algorandBaseViewModel.accountStateFlow.collectAsStateWithLifecycle()
    val storedAccountAddress by algorandBaseViewModel.storedAccountAddress.collectAsStateWithLifecycle()
    val isWalletChecked by algorandBaseViewModel.isWalletChecked.collectAsStateWithLifecycle()
    val isCreatingAccount by algorandBaseViewModel.isCreatingAccount.collectAsStateWithLifecycle()
    val walletSheet = rememberWalletSDKBottomSheetState()
    if (account != null) {
        // already logged in
        AlgorandExperienceTabs(navController, algorandBaseViewModel)
        // navController.navigate(Screen.AccountScreen.route)
        return
    } else {
        // showSnackbar(stringResource(resource = Res.string.error_account))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
    ) {
        Text(
            stringResource(resource = Res.string.login_guest_message),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            style =
                TextStyle(
                    fontSize = 24.sp,
                ),
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .width(300.dp)
                    .wrapContentHeight(),
        )
        Image(
            painter = painterResource(Res.drawable.coin_heads),
            contentDescription = Res.string.login_guest_message.toString(),
            modifier =
                Modifier
                    .wrapContentSize()
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .size(100.dp),
        )
        AlgorandDivider()
        // One account at a time: a stored account must be unlocked and signed out of before another is added.
        val address = storedAccountAddress
        if (isWalletChecked && address != null) {
            AlgorandButton(stringResourceId = Res.string.login_button_unlock, onClick = {
                algorandBaseViewModel.onUnlockWallet()
            })
            Text(
                truncatedAlgorandAddress(address),
                color = Color.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.alpha(0.6F),
            )
        } else if (isWalletChecked) {
            // Create stores a new Falcon25 account directly; Restore runs in the wallet-sdk-ui sheet.
            // Either way the keys stay encrypted in the SDK instead of being handled by this app.
            AlgorandButton(
                stringResourceId = Res.string.login_button_create,
                enabled = !isCreatingAccount,
                onClick = algorandBaseViewModel::onCreateAccount,
            )
            AlgorandButton(stringResourceId = Res.string.login_button_restore, onClick = {
                algorandBaseViewModel.onOnboardingStarted()
                walletSheet.show(initialScreen = AlgoKitScreens.FALCON25_RECOVER_PHRASE_SCREEN)
            })
        }
        AlgorandDivider()
        Text(
            stringResource(resource = Res.string.login_disclaimer),
            color = Color.Black,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .width(262.dp)
                    .wrapContentHeight()
                    .alpha(0.6F),
        )
    }

    AlgoKitTheme {
        WalletSDK.ShowAlgoKitWalletBottomSheet(
            state = walletSheet,
            onDismiss = { algorandBaseViewModel.onOnboardingFinished() },
        )
    }
}

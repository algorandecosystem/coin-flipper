package io.github.algorandecosystem.example.coinflipper.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.github.algorandecosystem.example.coinflipper.ui.navigation.Screen
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AlgorandBaseViewModel
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.ValidatorSearchViewModel
import io.github.algorandecosystem.example.coinflipper.ui.widgets.ValidatorSearchWidget
import org.koin.compose.getKoin

@Composable
fun ValidatorSearchScreen(
    navController: NavController,
    algorandBaseViewModel: AlgorandBaseViewModel,
    tag: String,
) {
    val validatorSearchViewModel: ValidatorSearchViewModel = getKoin().get()
    validatorSearchViewModel.algorandBaseViewModel = algorandBaseViewModel

    ValidatorSearchWidget(
        onValidatorSelected = { validatorId ->
            navController.navigate(Screen.ValidatorDetailScreen.route + "/$validatorId")
        },
    )
}

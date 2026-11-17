package io.github.algorandecosystem.example.coinflipper.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import io.github.algorandecosystem.example.coinflipper.ui.screens.AccountScreen
import io.github.algorandecosystem.example.coinflipper.ui.screens.HomeScreen
import io.github.algorandecosystem.example.coinflipper.ui.screens.LoginScreen
import io.github.algorandecosystem.example.coinflipper.ui.screens.PlayCoinFlipperScreen
import io.github.algorandecosystem.example.coinflipper.ui.screens.ValidatorDetailScreen
import io.github.algorandecosystem.example.coinflipper.ui.screens.ValidatorSearchScreen
import io.github.algorandecosystem.example.coinflipper.ui.viewmodels.AlgorandBaseViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar()
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier)
        },
        bottomBar = {
            NavigationBar {
                bottomNavigationItems().forEachIndexed { _, navigationItem ->
                    NavigationBarItem(
                        selected = navigationItem.route == currentDestination?.route,
                        label = {
                            Text(navigationItem.label)
                        },
                        icon = {
                            Icon(
                                navigationItem.icon,
                                contentDescription = navigationItem.label,
                            )
                        },
                        onClick = {
                            navController.navigate(navigationItem.route) {
                                popUpTo(navController.graph.findStartDestination().navigatorName) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.HomeScreen.route,
            route = "parentRoute",
            modifier = Modifier.padding(paddingValues = paddingValues),
        ) {
            composable(Screen.HomeScreen.route) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                HomeScreen(
                    tag = Screen.HomeScreen.route,
                    navController = navController,
                    algorandBaseViewModel = sharedViewModel,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
            composable(Screen.AccountScreen.route) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                AccountScreen(
                    tag = Screen.AccountScreen.route,
                    navController = navController,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
            composable(Screen.LoginScreen.route) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                LoginScreen(
                    tag = Screen.LoginScreen.route,
                    navController = navController,
                    algorandBaseViewModel = sharedViewModel,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
            composable(Screen.PlayCoinFlipperScreen.route) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                PlayCoinFlipperScreen(
                    tag = Screen.PlayCoinFlipperScreen.route,
                    navController = navController,
                    algorandBaseViewModel = sharedViewModel,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
            composable(Screen.ValidatorSearchScreen.route) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                ValidatorSearchScreen(
                    tag = Screen.ValidatorSearchScreen.route,
                    navController = navController,
                    algorandBaseViewModel = sharedViewModel,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
            composable(
                Screen.ValidatorDetailScreen.route + "/{validatorId}",
                arguments = listOf(navArgument("validatorId") { type = NavType.IntType }),
            ) {
                val backStackEntry = remember(it) { navController.getBackStackEntry("parentRoute") }
                val sharedViewModel: AlgorandBaseViewModel = koinInject()
                ValidatorDetailScreen(
                    tag = Screen.ValidatorDetailScreen.route,
                    navController = navController,
                    algorandBaseViewModel = sharedViewModel,
                    validatorId = it.arguments?.read { getInt("validatorId") } ?: 0,
                )
                SnackBarLayout(sharedViewModel, snackbarHostState)
            }
        }
    }
}

@Composable
fun SnackBarLayout(
    algorandBaseViewModel: AlgorandBaseViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val message by algorandBaseViewModel.snackBarMessage.collectAsStateWithLifecycle()
    // Keyed on the message so every new message is shown, even if one is already on screen
    // (e.g. the settle result arriving while "please wait" is still showing).
    LaunchedEffect(message) {
        if (message.isBlank()) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        // Only clear the message this effect showed, never a newer one.
        if (algorandBaseViewModel.snackBarMessage.value == message) {
            algorandBaseViewModel.setSnackBarMessage("")
        }
    }
}

fun bottomNavigationItems(): List<BottomNavigationItem> =
    listOf(
        BottomNavigationItem(
            label = "Home",
            icon = Icons.Filled.Home,
            route = Screen.HomeScreen.route,
        ),
        BottomNavigationItem(
            label = "Algorand Experience",
            icon = Icons.Filled.Star,
            route = Screen.LoginScreen.route,
        ),
    )

data class BottomNavigationItem(
    val label: String = "",
    val icon: ImageVector = Icons.Filled.Home,
    val route: String = "",
)

sealed class Screen(
    val route: String,
) {
    object HomeScreen : Screen("home")

    object AccountScreen : Screen("account")

    object LoginScreen : Screen("algorand_experience")

    object PlayCoinFlipperScreen : Screen("play_coin_flipper")

    object ValidatorDetailScreen : Screen("validator_detail")

    object ValidatorSearchScreen : Screen("validator_search")
}

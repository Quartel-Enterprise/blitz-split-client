package com.quare.blitzsplit.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchRootViewModel
import com.quare.blitzsplit.wear.ui.balances.WatchBalancesScreen
import com.quare.blitzsplit.wear.ui.balances.WatchGroupScreen
import com.quare.blitzsplit.wear.ui.pairing.WatchPairingScreen
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * Why: the watch draws the same shared back stack as the phone, through Navigation 3 with Wear's swipe to dismiss:
 * the connect screens until the watch has the account, then the balances and a group over them.
 */
@Composable
fun WearRoot(
    rootViewModel: WatchRootViewModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val isReady by rootViewModel.isReady.collectAsStateWithLifecycle()
    val backStack by navigator.backStack.collectAsStateWithLifecycle()
    WearTheme {
        AppScaffold {
            // Why: the stack holds the phone's start route until the root picks the watch's first screen.
            if (!isReady) return@AppScaffold
            NavDisplay(
                backStack = backStack,
                onBack = navigator::navigateBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
                entryProvider = entryProvider {
                    entry<Route.WatchConnect> { WatchPairingScreen() }
                    entry<Route.WatchBalances> { WatchBalancesScreen() }
                    entry<Route.WatchGroup> { route ->
                        WatchGroupScreen(viewModel = koinViewModel(key = route.toString()) { parametersOf(route) })
                    }
                },
            )
        }
    }
}

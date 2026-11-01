package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.copyText
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Why: the home's Saldos tab, each person once, netted across every group.
@Composable
fun GlobalBalancesScreen(
    route: Route.GlobalBalances,
    viewModel: GlobalBalancesViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is GlobalBalancesUiAction.ShareMessage -> context.shareText(action.message)

                is GlobalBalancesUiAction.CopyMessage -> {
                    context.copyText(action.message)
                    scope.showReplacingSnackbar(snackbarHostState, resources.getString(R.string.global_balances_copied))
                }

                GlobalBalancesUiAction.ShowComingSoon -> scope.showReplacingSnackbar(
                    snackbarHostState,
                    resources.getString(R.string.global_balances_coming_soon),
                )

                GlobalBalancesUiAction.ShowChangeError -> scope.showReplacingSnackbar(
                    snackbarHostState,
                    resources.getString(R.string.global_balances_change_error),
                )
            }
        }
    }
    // Why: Registrar pagamento, which "Acertar" or "Recebi" opened over Saldos, just closed.
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(GlobalBalancesUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(GlobalBalancesUiEvent.OnRecordedPaymentNoticeShown) },
    )
    GlobalBalancesContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    state.charge?.let { charge ->
        ChargeSheet(
            charge = charge,
            pixKey = state.pixKey,
            onEvent = viewModel::onEvent,
        )
    }
}

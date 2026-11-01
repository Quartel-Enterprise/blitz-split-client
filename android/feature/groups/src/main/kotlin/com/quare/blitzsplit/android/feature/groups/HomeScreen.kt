package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: Início, the root of the back stack. The recovery code sheet comes in as a slot, as on Grupos, since features
 * never see each other.
 */
@Composable
fun HomeScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            HomeUiAction.ShowComingSoon -> R.string.groups_coming_soon
            HomeUiAction.ShowChangeError -> R.string.groups_change_error
        }
    }
    // Why: "Registrar pagamento", which a shortcut or a face opened over Início, just closed.
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown) },
    )
    DeletedGroupNoticeEffect(
        notice = state.deletedGroupNotice,
        snackbarHostState = snackbarHostState,
        onShown = { viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown) },
    )
    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}

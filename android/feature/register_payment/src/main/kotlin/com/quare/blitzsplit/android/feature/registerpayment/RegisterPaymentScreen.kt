package com.quare.blitzsplit.android.feature.registerpayment

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DateTimePickerDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.SheetDismissLockEffect
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * Why: "Registrar pagamento", a payment made outside the app, over the group's screen; or, from the global Saldos
 * (Route.SettleUp), settling up with someone across groups.
 */
@Composable
fun RegisterPaymentScreen(
    route: Route,
    // Why: a sheet on a phone, a dialog on a tablet and a foldable (a book's right half), full screen on a tabletop.
    presentation: FormPresentation,
    viewModel: RegisterPaymentViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            RegisterPaymentUiAction.ShowSaveError -> R.string.register_payment_save_error
        }
    }
    var isPickingPaidAt by rememberSaveable { mutableStateOf(false) }
    BackHandler { viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked) }
    // Why: with unsaved input a drag down or the scrim asks "Discard changes?" instead of closing the sheet.
    SheetDismissLockEffect(
        isLocked = state.isSaving || state.hasUnsavedInput,
        onBlockedDismiss = if (state.isSaving) null else ({ viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked) }),
    )
    RegisterPaymentContent(
        state = state,
        presentation = presentation,
        snackbarHostState = snackbarHostState,
        onWhenClick = {
            viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtOpened)
            isPickingPaidAt = true
        },
        onEvent = viewModel::onEvent,
    )
    if (isPickingPaidAt) {
        DateTimePickerDialog(
            epochMillis = state.paidAt,
            latestMillis = state.latestPaidAt,
            onPicked = { millis ->
                isPickingPaidAt = false
                viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(millis))
            },
            onDismiss = { isPickingPaidAt = false },
        )
    }
}

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
import com.quare.blitzsplit.android.designsystem.layout.SheetDismissLockEffect
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Why: "Registrar pagamento", a payment made outside the app, over the group's screen.
@Composable
fun RegisterPaymentScreen(
    route: Route.RegisterPayment,
    // Why: a tablet and the upright foldable draw it as a dialog; a phone as a bottom sheet.
    isDialog: Boolean,
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
    SheetDismissLockEffect(isLocked = state.isSaving)
    RegisterPaymentContent(
        state = state,
        isDialog = isDialog,
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

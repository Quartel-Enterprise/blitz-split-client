package com.quare.blitzsplit.android.feature.payingmethod

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiEvent
import com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel.PayingMethodViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PayingMethodScreen(
    route: Route.PayingMethod,
    // Why: a tablet opens the method as a dialog over the profile, as it does a key, so the screen drops its scaffold.
    isDialog: Boolean,
    viewModel: PayingMethodViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(PayingMethodUiEvent.OnBackClicked) }
    PayingMethodContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
    if (state.isDeleteConfirmationShown) {
        ConfirmDialog(
            title = stringResource(R.string.paying_method_delete_title),
            message = stringResource(R.string.paying_method_delete_body),
            confirmLabel = stringResource(R.string.paying_method_delete_confirm),
            onConfirm = { viewModel.onEvent(PayingMethodUiEvent.OnDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(PayingMethodUiEvent.OnDeleteDismissed) },
        )
    }
}

package com.quare.blitzsplit.android.feature.splitbill

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DateTimePickerDialog
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SplitBillScreen(
    route: Route.SplitBill,
    // Why: a tablet and the upright foldable keep the group behind the bill, as a dialog.
    isDialog: Boolean,
    viewModel: SplitBillViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            SplitBillUiAction.ShowSaveError -> R.string.split_bill_save_error
            SplitBillUiAction.ShowDeleteError -> R.string.split_bill_delete_error
        }
    }
    var isPickingSpentAt by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        viewModel.onEvent(
            if (state.splitEditor == null) SplitBillUiEvent.OnCloseClicked else SplitBillUiEvent.OnSplitDismissed,
        )
    }
    AnimatedContent(
        targetState = state.splitEditor,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it != null },
        label = "split-editor",
    ) { editor ->
        if (editor == null) {
            SplitBillContent(
                state = state,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onWhenClick = {
                    viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)
                    isPickingSpentAt = true
                },
                onEvent = viewModel::onEvent,
            )
        } else {
            SplitEditorContent(
                state = state,
                editor = editor,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        }
    }
    state.groupSelection?.let { selection ->
        GroupSheet(
            groups = state.groups,
            selection = selection,
            onEvent = viewModel::onEvent,
        )
    }
    state.payerSelection?.let { selection ->
        PayerSheet(
            state = state,
            selection = selection,
            money = rememberMoneyFormatter(state.currency),
            onEvent = viewModel::onEvent,
        )
    }
    state.payerAmounts?.let { amounts ->
        SeveralPayersSheet(
            amounts = amounts,
            money = rememberMoneyFormatter(state.currency),
            onEvent = viewModel::onEvent,
        )
    }
    state.deletion?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            isDeleting = state.isDeleting,
            money = rememberMoneyFormatter(state.currency),
            onConfirm = { viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(SplitBillUiEvent.OnDeleteDismissed) },
        )
    }
    if (isPickingSpentAt) {
        DateTimePickerDialog(
            epochMillis = state.spentAt,
            latestMillis = state.latestSpentAt,
            onPicked = { millis ->
                isPickingSpentAt = false
                viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(millis))
            },
            onDismiss = { isPickingSpentAt = false },
        )
    }
}

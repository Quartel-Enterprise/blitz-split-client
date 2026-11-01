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

private const val PAGE_BILL = 0
private const val PAGE_SPLIT = 1
private const val PAGE_ITEMS = 2
private const val PAGE_ITEM = 3

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
            when {
                state.itemEditor != null -> SplitBillUiEvent.OnItemDismissed
                state.itemsEditor != null -> SplitBillUiEvent.OnItemsDismissed
                state.splitEditor != null -> SplitBillUiEvent.OnSplitDismissed
                else -> SplitBillUiEvent.OnCloseClicked
            },
        )
    }
    // Why: Dividir over the bill, "Itens da conta" in Dividir's place and "Item" over it, each kept while it fades out.
    AnimatedContent(
        targetState = Triple(state.splitEditor, state.itemsEditor, state.itemEditor),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { (split, items, item) ->
            when {
                item != null -> PAGE_ITEM
                items != null -> PAGE_ITEMS
                split != null -> PAGE_SPLIT
                else -> PAGE_BILL
            }
        },
        label = "split-editor",
    ) { (editor, items, item) ->
        if (item != null) {
            ItemEditorContent(
                editor = item,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (items != null) {
            ItemsEditorContent(
                state = state,
                items = items,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (editor == null) {
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
    state.itemDeletion?.let { deletion ->
        DeleteItemDialog(
            deletion = deletion,
            money = rememberMoneyFormatter(state.currency),
            onEvent = viewModel::onEvent,
        )
    }
    state.feeEditor?.let { editor ->
        FeeDialog(
            editor = editor,
            money = rememberMoneyFormatter(state.currency),
            onEvent = viewModel::onEvent,
        )
    }
    state.deletion?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            settledPair = deletion.settledPair,
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

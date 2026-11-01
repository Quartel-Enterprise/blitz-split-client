package com.quare.blitzsplit.android.feature.splitbill

import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryForm
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryPicker
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
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
private const val PAGE_CATEGORY = 4
private const val PAGE_CATEGORY_FORM = 5

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
            SplitBillUiAction.ShowCategorySaveError -> R.string.split_bill_category_save_error
            SplitBillUiAction.ShowCategoryDeleteError -> R.string.split_bill_category_delete_error
        }
    }
    var isPickingSpentAt by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        viewModel.onEvent(
            when {
                state.categoryForm != null -> SplitBillUiEvent.OnCategoryFormDismissed
                state.categoryPicker != null -> SplitBillUiEvent.OnCategoryDismissed
                state.itemEditor != null -> SplitBillUiEvent.OnItemDismissed
                state.itemsEditor != null -> SplitBillUiEvent.OnItemsDismissed
                state.splitEditor != null -> SplitBillUiEvent.OnSplitDismissed
                else -> SplitBillUiEvent.OnCloseClicked
            },
        )
    }
    /*
     * Why: Dividir over the bill, "Itens da conta" in Dividir's place and "Item" over it, "Categoria" and its form in
     * the bill's place, each kept while it fades out.
     */
    AnimatedContent(
        targetState = BillPages(
            split = state.splitEditor,
            items = state.itemsEditor,
            item = state.itemEditor,
            categoryPicker = state.categoryPicker,
            categoryForm = state.categoryForm,
        ),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { pages ->
            when {
                pages.categoryForm != null -> PAGE_CATEGORY_FORM
                pages.categoryPicker != null -> PAGE_CATEGORY
                pages.item != null -> PAGE_ITEM
                pages.items != null -> PAGE_ITEMS
                pages.split != null -> PAGE_SPLIT
                else -> PAGE_BILL
            }
        },
        label = "split-editor",
    ) { pages ->
        val editor = pages.split
        val items = pages.items
        val item = pages.item
        val categoryForm = pages.categoryForm
        val categoryPicker = pages.categoryPicker
        if (categoryForm != null) {
            CategoryFormContent(
                state = state,
                form = categoryForm,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (categoryPicker != null) {
            CategoryPickerContent(
                state = state,
                picker = categoryPicker,
                isDialog = isDialog,
                snackbarHostState = snackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (item != null) {
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
    state.categoryDeletion?.let { deletion ->
        DeleteCategoryDialog(
            state = state,
            deletion = deletion,
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

// Why: what covers the bill, from the deepest: the category's form, the picker, an item, the items, Dividir.
private data class BillPages(
    val split: SplitEditor?,
    val items: ItemsEditor?,
    val item: ItemEditor?,
    val categoryPicker: CategoryPicker?,
    val categoryForm: CategoryForm?,
)

package com.quare.blitzsplit.android.feature.splitbill

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.CategoryFormActions
import com.quare.blitzsplit.android.designsystem.component.CategoryFormContent
import com.quare.blitzsplit.android.designsystem.component.DateTimePickerDialog
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.DeleteCategoryDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesTabletopHalves
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryPicker
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

private const val PAGE_BILL = 0
private const val PAGE_SPLIT = 1
private const val PAGE_ITEMS = 2
private const val PAGE_ITEM = 3
private const val PAGE_CATEGORY = 4
private const val PAGE_CATEGORY_FORM = 5

@Composable
fun SplitBillScreen(
    route: Route.SplitBill,
    // Why: anywhere wider than a phone the group stays behind the bill, as a dialog, but under a new bill on a tabletop.
    isDialog: Boolean,
    viewModel: SplitBillViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            SplitBillUiAction.ShowSaveError -> R.string.split_bill_save_error
            SplitBillUiAction.ShowDeleteError -> R.string.split_bill_delete_error
            SplitBillUiAction.ShowCategorySaveError -> DesignR.string.designsystem_category_save_error
            SplitBillUiAction.ShowCategoryDeleteError -> DesignR.string.designsystem_category_delete_error
        }
    }
    var isPickingSpentAt by rememberSaveable { mutableStateOf(false) }
    BackHandler { viewModel.onEvent(state.toBackEvent()) }
    /*
     * Why: a new bill on a tabletop splits on the hinge, but Item, Categoria and Nova categoria have no tabletop board
     * of their own: they open as the book draws them, a dialog in a half, over the bill or the items under them.
     */
    val opensPagesInHalf = rememberUsesTabletopHalves(isDialog)
    val billPages = BillPages(
        split = state.splitEditor,
        items = state.itemsEditor,
        item = state.itemEditor,
        categoryPicker = state.categoryPicker,
        categoryForm = state.categoryForm,
    )
    // Why: a page in the half shows its snackbar alone, rather than once more under its scrim.
    val idleSnackbarHostState = remember { SnackbarHostState() }
    val pagesSnackbarHostState = if (opensPagesInHalf && billPages.hasHalfPage) {
        idleSnackbarHostState
    } else {
        snackbarHostState
    }
    /*
     * Why: Dividir over the bill, "Itens da conta" in Dividir's place and "Item" over it, "Categoria" and its form in
     * the bill's place, each kept while it fades out.
     */
    AnimatedContent(
        targetState = if (opensPagesInHalf) billPages.toPagesUnderHalf() else billPages,
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
            BillCategoryFormContent(
                state = state,
                form = categoryForm,
                isDialog = isDialog,
                snackbarHostState = pagesSnackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (categoryPicker != null) {
            CategoryPickerContent(
                state = state,
                picker = categoryPicker,
                isDialog = isDialog,
                snackbarHostState = pagesSnackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (item != null) {
            ItemEditorContent(
                editor = item,
                isDialog = isDialog,
                snackbarHostState = pagesSnackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (items != null) {
            ItemsEditorContent(
                state = state,
                items = items,
                isDialog = isDialog,
                snackbarHostState = pagesSnackbarHostState,
                onEvent = viewModel::onEvent,
            )
        } else if (editor == null) {
            SplitBillContent(
                state = state,
                isDialog = isDialog,
                snackbarHostState = pagesSnackbarHostState,
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
                snackbarHostState = pagesSnackbarHostState,
                onEvent = viewModel::onEvent,
            )
        }
    }
    if (opensPagesInHalf && billPages.hasHalfPage) {
        FoldDialog(
            onDismissRequest = { viewModel.onEvent(state.toBackEvent()) },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            HalfPageContent(
                state = state,
                pages = billPages,
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
            deletion = deletion,
            groupName = state.groupName,
            onConfirm = { viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteDismissed) },
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
    if (state.showsReceiptCamera) {
        ReceiptCameraDialog(
            onPhoto = { photo -> viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(photo)) },
            onDismiss = { viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraDismissed) },
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

// Why: back closes the deepest page over the bill, then the bill; a tabletop's half page closes the same way.
private fun SplitBillUiState.toBackEvent(): SplitBillUiEvent = when {
    categoryForm != null -> SplitBillUiEvent.OnCategoryFormDismissed
    categoryPicker != null -> SplitBillUiEvent.OnCategoryDismissed
    itemEditor != null -> SplitBillUiEvent.OnItemDismissed
    itemsEditor != null -> SplitBillUiEvent.OnItemsDismissed
    splitEditor != null -> SplitBillUiEvent.OnSplitDismissed
    else -> SplitBillUiEvent.OnCloseClicked
}

// Why: Item, Categoria or Nova categoria in a tabletop's half, drawn as the dialog they are on a book.
@Composable
private fun HalfPageContent(
    state: SplitBillUiState,
    pages: BillPages,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val item = pages.item
    val categoryPicker = pages.categoryPicker
    val categoryForm = pages.categoryForm
    when {
        categoryForm != null -> BillCategoryFormContent(
            state = state,
            form = categoryForm,
            isDialog = true,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
        )

        categoryPicker != null -> CategoryPickerContent(
            state = state,
            picker = categoryPicker,
            isDialog = true,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
        )

        item != null -> ItemEditorContent(
            editor = item,
            isDialog = true,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
        )
    }
}

// Why: "Nova categoria" (7.8) in the picker's place, the form the map opens too.
@Composable
private fun BillCategoryFormContent(
    state: SplitBillUiState,
    form: CategoryForm,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    CategoryFormContent(
        editor = state.categoryEditorOf(form),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        actions = remember(onEvent) {
            CategoryFormActions(
                onNameChanged = { onEvent(SplitBillUiEvent.OnCategoryNameChanged(it)) },
                onParentPicked = { onEvent(SplitBillUiEvent.OnCategoryParentPicked(it)) },
                onIconPicked = { onEvent(SplitBillUiEvent.OnCategoryIconPicked(it)) },
                onSave = { onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked) },
                onDismiss = { onEvent(SplitBillUiEvent.OnCategoryFormDismissed) },
                onDelete = { onEvent(SplitBillUiEvent.OnCategoryDeleteClicked) },
            )
        },
    )
}

// Why: what covers the bill, from the deepest: the category's form, the picker, an item, the items, Dividir.
private data class BillPages(
    val split: SplitEditor?,
    val items: ItemsEditor?,
    val item: ItemEditor?,
    val categoryPicker: CategoryPicker?,
    val categoryForm: CategoryForm?,
) {
    val hasHalfPage: Boolean
        get() = item != null || categoryPicker != null || categoryForm != null

    // Why: what stays under the half on a tabletop, the bill or the items.
    fun toPagesUnderHalf(): BillPages = copy(
        item = null,
        categoryPicker = null,
        categoryForm = null,
    )
}

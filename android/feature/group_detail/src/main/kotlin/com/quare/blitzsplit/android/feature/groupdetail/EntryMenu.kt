package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.component.RowMenuButton
import com.quare.blitzsplit.android.designsystem.component.RowMenuItem
import com.quare.blitzsplit.android.designsystem.component.billMenuItemsOf
import com.quare.blitzsplit.android.designsystem.component.deleteMenuItemOf
import com.quare.blitzsplit.android.designsystem.component.rowOptionsLabelOf
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent

// Why: what Conta's top bar offers, in the same order (plus Duplicar), as Início's rows offer it too.
@Composable
internal fun expenseMenuItemsOf(
    expenseId: String,
    onEvent: (GroupDetailUiEvent) -> Unit,
): List<RowMenuItem> = billMenuItemsOf(
    onEdit = { onEvent(GroupDetailUiEvent.OnExpenseEditClicked(expenseId)) },
    onDuplicate = { onEvent(GroupDetailUiEvent.OnExpenseDuplicateClicked(expenseId)) },
    onShare = { onEvent(GroupDetailUiEvent.OnExpenseShareClicked(expenseId)) },
    onDelete = { onEvent(GroupDetailUiEvent.OnExpenseDeleteClicked(expenseId)) },
)

// Why: a payment has no screen of its own: deleting it, which asks first, is all there is to do with it.
@Composable
internal fun paymentMenuItemsOf(
    paymentId: String,
    onEvent: (GroupDetailUiEvent) -> Unit,
): List<RowMenuItem> = listOf(deleteMenuItemOf { onEvent(GroupDetailUiEvent.OnPaymentDeleteClicked(paymentId)) })

// Why: the row's ⋮, which a long press on the row opens too, drawn as every list row's ⋮ is.
@Composable
internal fun EntryMenu(
    title: String,
    items: List<RowMenuItem>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
) {
    RowMenuButton(
        contentDescription = rowOptionsLabelOf(title),
        items = items,
        isOpen = isOpen,
        onOpenChange = onOpenChange,
    )
}

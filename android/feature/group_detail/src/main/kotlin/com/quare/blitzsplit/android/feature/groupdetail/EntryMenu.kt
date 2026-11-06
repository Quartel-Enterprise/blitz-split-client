package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.component.RowMenuButton
import com.quare.blitzsplit.android.designsystem.component.RowMenuItem
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent

// Why: one option of a bill's or a payment's ⋮ on Despesas; a destructive one is drawn in the error color.
internal class EntryMenuItem(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean,
    val event: GroupDetailUiEvent,
)

// Why: what Conta's top bar offers, in the same order, so the bill's options don't need it opened first.
@Composable
internal fun expenseMenuItemsOf(expenseId: String): List<EntryMenuItem> = listOf(
    EntryMenuItem(
        label = stringResource(R.string.group_detail_entry_edit),
        icon = Icons.Outlined.Edit,
        isDestructive = false,
        event = GroupDetailUiEvent.OnExpenseEditClicked(expenseId),
    ),
    EntryMenuItem(
        label = stringResource(R.string.group_detail_entry_share),
        icon = Icons.Outlined.Share,
        isDestructive = false,
        event = GroupDetailUiEvent.OnExpenseShareClicked(expenseId),
    ),
    EntryMenuItem(
        label = stringResource(R.string.group_detail_entry_delete),
        icon = Icons.Outlined.Delete,
        isDestructive = true,
        event = GroupDetailUiEvent.OnExpenseDeleteClicked(expenseId),
    ),
)

// Why: a payment has no screen of its own: deleting it, which asks first, is all there is to do with it.
@Composable
internal fun paymentMenuItemsOf(paymentId: String): List<EntryMenuItem> = listOf(
    EntryMenuItem(
        label = stringResource(R.string.group_detail_entry_delete),
        icon = Icons.Outlined.Delete,
        isDestructive = true,
        event = GroupDetailUiEvent.OnPaymentDeleteClicked(paymentId),
    ),
)

// Why: the row's ⋮, which a long press on the row opens too, drawn as every list row's ⋮ is.
@Composable
internal fun EntryMenu(
    title: String,
    items: List<EntryMenuItem>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    RowMenuButton(
        contentDescription = stringResource(R.string.group_detail_entry_options, title),
        items = items.map { item ->
            RowMenuItem(
                label = item.label,
                icon = item.icon,
                hint = null,
                isEnabled = true,
                isDestructive = item.isDestructive,
                onClick = { onEvent(item.event) },
            )
        },
        isOpen = isOpen,
        onOpenChange = onOpenChange,
    )
}

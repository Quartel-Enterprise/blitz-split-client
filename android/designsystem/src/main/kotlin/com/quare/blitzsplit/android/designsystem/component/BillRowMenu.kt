package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R

/*
 * Why: a bill's ⋮ on the group's Despesas, Início's "Últimas despesas" and "Todas as despesas": what Conta's top bar
 * offers, in the same order (plus Duplicar), so the bill's options don't need it opened first.
 */
@Composable
fun billMenuItemsOf(
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
): List<RowMenuItem> {
    val editLabel = stringResource(R.string.designsystem_bill_menu_edit)
    val duplicateLabel = stringResource(R.string.designsystem_bill_menu_duplicate)
    val shareLabel = stringResource(R.string.designsystem_bill_menu_share)
    val deleteItem = deleteMenuItemOf(onDelete)
    // Why: a row of a long list hands the same list to its menu until one of its parts changes, so the menu skips.
    return remember(editLabel, duplicateLabel, shareLabel, deleteItem, onEdit, onDuplicate, onShare) {
        listOf(
            RowMenuItem(
                label = editLabel,
                icon = Icons.Outlined.Edit,
                hint = null,
                isEnabled = true,
                isDestructive = false,
                onClick = onEdit,
            ),
            RowMenuItem(
                label = duplicateLabel,
                icon = Icons.Outlined.ContentCopy,
                hint = null,
                isEnabled = true,
                isDestructive = false,
                onClick = onDuplicate,
            ),
            RowMenuItem(
                label = shareLabel,
                icon = Icons.Outlined.Share,
                hint = null,
                isEnabled = true,
                isDestructive = false,
                onClick = onShare,
            ),
            deleteItem,
        )
    }
}

// Why: a payment's only option, and a bill's last: deleting, which asks first, drawn in the error color.
@Composable
fun deleteMenuItemOf(onDelete: () -> Unit): RowMenuItem {
    val label = stringResource(R.string.designsystem_bill_menu_delete)
    return remember(label, onDelete) {
        RowMenuItem(
            label = label,
            icon = Icons.Outlined.Delete,
            hint = null,
            isEnabled = true,
            isDestructive = true,
            onClick = onDelete,
        )
    }
}

// Why: "Opções de Mercado", what TalkBack says for the row's ⋮ and its long press.
@Composable
fun rowOptionsLabelOf(title: String): String = stringResource(R.string.designsystem_bill_menu_options, title)

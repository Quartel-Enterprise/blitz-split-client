package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
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
): List<RowMenuItem> = listOf(
    RowMenuItem(
        label = stringResource(R.string.designsystem_bill_menu_edit),
        icon = Icons.Outlined.Edit,
        hint = null,
        isEnabled = true,
        isDestructive = false,
        onClick = onEdit,
    ),
    RowMenuItem(
        label = stringResource(R.string.designsystem_bill_menu_duplicate),
        icon = Icons.Outlined.ContentCopy,
        hint = null,
        isEnabled = true,
        isDestructive = false,
        onClick = onDuplicate,
    ),
    RowMenuItem(
        label = stringResource(R.string.designsystem_bill_menu_share),
        icon = Icons.Outlined.Share,
        hint = null,
        isEnabled = true,
        isDestructive = false,
        onClick = onShare,
    ),
    deleteMenuItemOf(onDelete),
)

// Why: a payment's only option, and a bill's last: deleting, which asks first, drawn in the error color.
@Composable
fun deleteMenuItemOf(onDelete: () -> Unit): RowMenuItem = RowMenuItem(
    label = stringResource(R.string.designsystem_bill_menu_delete),
    icon = Icons.Outlined.Delete,
    hint = null,
    isEnabled = true,
    isDestructive = true,
    onClick = onDelete,
)

// Why: "Opções de Mercado", what TalkBack says for the row's ⋮ and its long press.
@Composable
fun rowOptionsLabelOf(title: String): String = stringResource(R.string.designsystem_bill_menu_options, title)

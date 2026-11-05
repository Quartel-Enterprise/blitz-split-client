package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

// Why: one option of a row's ⋮: a destructive one in the error color, a hint ("Precisa de internet") under its label.
class RowMenuItem(
    val label: String,
    val icon: ImageVector,
    val hint: String?,
    val isEnabled: Boolean,
    val isDestructive: Boolean,
    val onClick: () -> Unit,
)

/*
 * Why: the ⋮ at the end of a list's row (Grupos' groups, a group's bills and payments), the same everywhere. The
 * caller keeps whether it is open, so a long press on the row can open it too.
 */
@Composable
fun RowMenuButton(
    contentDescription: String,
    items: List<RowMenuItem>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
) {
    Box {
        IconButton(onClick = { onOpenChange(true) }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { onOpenChange(false) },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.label)
                            item.hint?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    onClick = {
                        onOpenChange(false)
                        item.onClick()
                    },
                    enabled = item.isEnabled,
                    leadingIcon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                        )
                    },
                    colors = if (item.isDestructive) {
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        MenuDefaults.itemColors()
                    },
                )
            }
        }
    }
}

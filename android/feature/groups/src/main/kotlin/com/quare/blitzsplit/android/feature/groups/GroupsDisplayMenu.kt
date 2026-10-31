package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

private enum class DisplayOption(
    @StringRes val label: Int,
    val icon: ImageVector,
    val display: GroupsDisplay,
) {
    LIST(R.string.groups_display_list, Icons.AutoMirrored.Outlined.ViewList, GroupsDisplay.LIST),
    GRID(R.string.groups_display_grid, Icons.Outlined.GridView, GroupsDisplay.GRID),
}

@StringRes
private fun gridSizeLabelOf(size: GroupGridSize): Int = when (size) {
    GroupGridSize.COMPACT -> R.string.groups_grid_size_compact
    GroupGridSize.STANDARD -> R.string.groups_grid_size_standard
    GroupGridSize.LARGE -> R.string.groups_grid_size_large
}

/*
 * Why: "Exibição" (5.5), the button of the "Em aberto" header: Lista or Grade and, below, the grid's size, disabled
 * while the list is picked. The choice is kept on the device.
 */
@Composable
internal fun GroupsDisplayMenu(
    layout: GroupsLayout,
    canPickGridSize: Boolean,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val isGrid = layout.display == GroupsDisplay.GRID
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = if (isGrid) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ViewList,
                contentDescription = stringResource(
                    if (isGrid) R.string.groups_display_grid_description else R.string.groups_display_list_description,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
        ) {
            DisplayOption.entries.forEach { option ->
                DisplayMenuItem(
                    label = option.label,
                    icon = option.icon,
                    isSelected = layout.display == option.display,
                    isEnabled = true,
                    onClick = {
                        isOpen = false
                        onEvent(GroupsUiEvent.OnDisplayChanged(option.display))
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.groups_grid_size),
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 4.dp,
                    ).semantics { heading() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GroupGridSize.entries.forEach { size ->
                val isSelected = layout.gridSize == size
                DisplayMenuItem(
                    label = gridSizeLabelOf(size),
                    icon = if (isSelected) Icons.Outlined.Check else null,
                    isSelected = isSelected,
                    isEnabled = canPickGridSize,
                    onClick = {
                        isOpen = false
                        onEvent(GroupsUiEvent.OnGridSizeChanged(size))
                    },
                )
            }
        }
    }
}

// Why: the picked option shows on the secondary container, as the canvas's menu does.
@Composable
private fun DisplayMenuItem(
    @StringRes label: Int,
    icon: ImageVector?,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(
                color = if (isSelected) colors.secondaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.medium,
            ).semantics {
                role = Role.RadioButton
                selected = isSelected
            },
        leadingIcon = {
            // Why: an unselected size keeps the check's room, so the labels line up.
            if (icon == null) {
                Spacer(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        enabled = isEnabled,
        colors = if (isSelected) {
            MenuDefaults.itemColors(
                textColor = colors.onSecondaryContainer,
                leadingIconColor = colors.onSecondaryContainer,
            )
        } else {
            MenuDefaults.itemColors()
        },
    )
}

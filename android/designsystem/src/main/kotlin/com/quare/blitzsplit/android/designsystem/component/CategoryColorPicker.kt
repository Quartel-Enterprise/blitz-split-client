package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorState
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.tint

/*
 * Why: the colors a category may take (7.7), a radio group so a screen reader names each and the picked one: the twelve
 * named ones, then the person's own (a long press asks to delete one), then "+", which opens "New color" (Pierre's beta
 * feedback of 2026-11-14). categoryKey is the app category "Change the color of Food" is open for, null in a form. The
 * dialogs of the person's colors draw here, over whichever picker is on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryColorPicker(
    selected: CategoryTint,
    onSelected: (CategoryTint) -> Unit,
    customColors: CustomColorState,
    onCustomColor: (CustomColorEvent) -> Unit,
    modifier: Modifier = Modifier,
    categoryKey: String? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val customSwatches = customColors.customSwatchesFor(selected)
    /*
     * Why: "New color" belongs to the picker that opened it; one that leaves the screen (its dialog closed by a rotation)
     * takes the draft along, so a later picker never opens on it nor sends its color to the earlier category.
     */
    val latestOnCustomColor by rememberUpdatedState(onCustomColor)
    DisposableEffect(Unit) {
        onDispose { latestOnCustomColor(CustomColorEvent.OnPickerClosed) }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryColor.entries.forEach { color ->
                val tint = color.tint
                ColorSwatchButton(
                    label = stringResource(color.labelRes),
                    isSelected = tint == selected,
                    container = tint.toContainerColor(isDark),
                    content = tint.toContentColor(isDark),
                    onClick = { onSelected(tint) },
                )
            }
            customSwatches.forEach { color ->
                val tint = color.tint
                val isDeletable = customColors.isDeletable(color)
                ColorSwatchButton(
                    label = stringResource(R.string.designsystem_custom_color_label, color.hex),
                    isSelected = tint == selected,
                    container = tint.toContainerColor(isDark),
                    content = tint.toContentColor(isDark),
                    onClick = { onSelected(tint) },
                    onLongClick = if (isDeletable) {
                        { onCustomColor(CustomColorEvent.OnLongPressed(color)) }
                    } else {
                        null
                    },
                    onLongClickLabel = stringResource(R.string.designsystem_custom_color_delete),
                )
            }
            if (customColors.canAdd) {
                OutlinedIconButton(
                    onClick = { onCustomColor(CustomColorEvent.OnAddClicked(selected, categoryKey)) },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.designsystem_custom_color_add),
                    )
                }
            }
        }
        val note = when {
            !customColors.canAdd -> R.string.designsystem_custom_color_full
            customColors.palette.isNotEmpty() -> R.string.designsystem_custom_color_hint
            else -> null
        }
        note?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    CustomColorOverlay(
        state = customColors,
        onEvent = onCustomColor,
    )
}

// Why: "New color", then "Delete this color?" or "This color is in use", over the picker that opened them.
@Composable
private fun CustomColorOverlay(
    state: CustomColorState,
    onEvent: (CustomColorEvent) -> Unit,
) {
    val placement = rememberDialogPlacement()
    state.draft?.let { draft ->
        CustomColorDialog(
            draft = draft,
            onEvent = onEvent,
            placement = placement,
        )
    }
    val deletion = state.deletion ?: return
    val usedBy = deletion.usedBy
    if (usedBy != null) {
        FoldAlertDialog(
            onDismissRequest = { onEvent(CustomColorEvent.OnDeletionDismissed) },
            title = stringResource(R.string.designsystem_custom_color_in_use_title),
            text = {
                Text(stringResource(R.string.designsystem_custom_color_in_use_message, categoryName(usedBy)))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CustomColorEvent.OnDeletionDismissed) }) {
                    Text(stringResource(R.string.designsystem_custom_color_ok))
                }
            },
            placement = placement,
        )
    } else {
        ConfirmDialog(
            title = stringResource(R.string.designsystem_custom_color_delete_title),
            message = stringResource(R.string.designsystem_custom_color_delete_message),
            confirmLabel = stringResource(R.string.designsystem_custom_color_delete),
            onConfirm = { onEvent(CustomColorEvent.OnDeleteConfirmed) },
            onDismiss = { onEvent(CustomColorEvent.OnDeletionDismissed) },
            isBusy = deletion.isDeleting,
            placement = placement,
        )
    }
}

@get:StringRes
val CategoryColor.labelRes: Int
    get() = when (this) {
        CategoryColor.BLUE -> R.string.designsystem_category_color_blue
        CategoryColor.ORANGE -> R.string.designsystem_category_color_orange
        CategoryColor.GREEN -> R.string.designsystem_category_color_green
        CategoryColor.PURPLE -> R.string.designsystem_category_color_purple
        CategoryColor.YELLOW -> R.string.designsystem_category_color_yellow
        CategoryColor.RED -> R.string.designsystem_category_color_red
        CategoryColor.TEAL -> R.string.designsystem_category_color_teal
        CategoryColor.PINK -> R.string.designsystem_category_color_pink
        CategoryColor.INDIGO -> R.string.designsystem_category_color_indigo
        CategoryColor.LIME -> R.string.designsystem_category_color_lime
        CategoryColor.BROWN -> R.string.designsystem_category_color_brown
        CategoryColor.GRAY -> R.string.designsystem_category_color_gray
    }

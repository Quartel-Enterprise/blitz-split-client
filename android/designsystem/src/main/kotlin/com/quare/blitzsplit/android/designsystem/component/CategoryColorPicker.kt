package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor

// Why: the twelve colors a category may take (7.7), a radio group so a screen reader names each and the picked one.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryColorPicker(
    selected: CategoryColor,
    onSelected: (CategoryColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryColor.entries.forEach { color ->
            val isSelected = color == selected
            val label = stringResource(color.labelRes)
            ColorSwatchButton(
                label = label,
                isSelected = isSelected,
                container = color.toContainerColor(isDark),
                content = color.toContentColor(isDark),
                onClick = { onSelected(color) },
            )
        }
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

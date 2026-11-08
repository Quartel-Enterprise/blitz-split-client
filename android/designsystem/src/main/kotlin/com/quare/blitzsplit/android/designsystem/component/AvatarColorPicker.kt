package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor

// Why: a radio group, so a screen reader names each color and says which one is picked.
@Composable
fun AvatarColorPicker(
    selected: AvatarColor,
    colors: List<AvatarColor>,
    onSelected: (AvatarColor) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
    ) {
        colors.forEach { color ->
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
val AvatarColor.labelRes: Int
    get() = when (this) {
        AvatarColor.BLUE -> R.string.designsystem_avatar_color_blue
        AvatarColor.PEACH -> R.string.designsystem_avatar_color_peach
        AvatarColor.GREEN -> R.string.designsystem_avatar_color_green
        AvatarColor.LILAC -> R.string.designsystem_avatar_color_lilac
        AvatarColor.SAND -> R.string.designsystem_avatar_color_sand
    }

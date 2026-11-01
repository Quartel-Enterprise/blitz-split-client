package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = label }
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelected(color) },
                    ),
                shape = CircleShape,
                color = color.toContainerColor(isDark),
                contentColor = color.toContentColor(isDark),
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null)
                }
            }
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

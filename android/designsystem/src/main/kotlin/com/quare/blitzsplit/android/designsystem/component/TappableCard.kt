package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/*
 * Why: Material's elevated card, level 1 at rest (pressed and focused too) and level 2 under a pointer, and its tonal
 * elevation at level 1, the surface tint over the container at 5 %.
 */
private val restingElevation = 1.dp
private val hoveredElevation = 3.dp
private const val RESTING_TINT_ALPHA = 0.05f

/*
 * Why: on Android a shadow means "tap me" (docs/product/general.md): a card that opens something as one touch target
 * is Material's elevated card, with its ripple, TalkBack's "button" and its hovered elevation (pressed stays at rest,
 * as Material's tokens say), in its screen's container color rather than the elevated card's own, so it matches the
 * rows beside it. A dark theme barely shows a shadow, so there the card also takes Material's tonal tint of its level.
 * onLongClick holds a card's actions (Groups' grid), on the same touch target.
 */
@Composable
fun TappableCard(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (isHovered) hoveredElevation else restingElevation)
    val colors = MaterialTheme.colorScheme
    val color = if (colors.surface.luminance() < 0.5f) {
        colors.surfaceTint.copy(alpha = RESTING_TINT_ALPHA).compositeOver(containerColor)
    } else {
        containerColor
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        shadowElevation = elevation,
    ) {
        Column(
            modifier = Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
            content = content,
        )
    }
}

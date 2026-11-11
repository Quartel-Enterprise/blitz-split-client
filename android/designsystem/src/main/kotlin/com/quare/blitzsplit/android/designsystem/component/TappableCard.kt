package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/*
 * Why: on Android a shadow means "tap me" (docs/product/general.md): a card that opens something as one touch target
 * is Material's elevated card, with its ripple and its hovered elevation (pressed stays at rest, as Material's tokens
 * say), in its screen's container color rather than the elevated card's own, so it matches the rows beside it.
 */
@Composable
fun TappableCard(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        content = content,
    )
}

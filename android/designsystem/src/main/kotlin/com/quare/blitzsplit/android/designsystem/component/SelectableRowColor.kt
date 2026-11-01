package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Why: a list and detail marks the row or card shown beside the list (a group in Grupos, a person in Saldos) with the
 * canvas' secondary container; the others keep the card color.
 */
@Composable
fun selectableRowColorOf(isSelected: Boolean): Color = if (isSelected) {
    MaterialTheme.colorScheme.secondaryContainer
} else {
    MaterialTheme.colorScheme.surfaceContainerLowest
}

package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/*
 * Why: one round swatch of a radio group, shared by the avatar and the category color pickers: the screen reader names
 * it, and the picked one has a ring and a check. One of the person's own colors also takes a long press (onLongClick,
 * labelled for the screen reader's actions), which asks to delete it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColorSwatchButton(
    label: String,
    isSelected: Boolean,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val interaction = if (onLongClick == null) {
        Modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        Modifier
            .semantics { selected = isSelected }
            .combinedClickable(
                role = Role.RadioButton,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
            )
    }
    Surface(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = label }
            .then(interaction),
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null)
        }
    }
}

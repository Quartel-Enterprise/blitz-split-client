package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

/*
 * Why: a group's tabs swap in place, the tab left still drawn under the next one for a moment, so the header never
 * blinks while the next tab reads the group. Until it has, the next tab is see-through, showing the one it replaces,
 * and keeps the taps from reaching it.
 */
@Composable
fun groupTabContainerColorOf(isLoaded: Boolean): Color = if (isLoaded) {
    MaterialTheme.colorScheme.background
} else {
    Color.Transparent
}

fun Modifier.blocksTapsWhileLoading(isLoaded: Boolean): Modifier = if (isLoaded) {
    this
} else {
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

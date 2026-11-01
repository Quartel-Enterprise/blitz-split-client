package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val halfPaneMargin = 16.dp

/*
 * Why: a dialog that keeps off the hinge. Centered it is a plain Dialog, as before; in a half its window covers the
 * screen, so the platform's scrim dims both halves, and the content sits centered in its half, at most the half minus
 * 16dp each side (the canvas' book dialog). Tapping outside it still dismisses, when the properties say so. The
 * content brings its own surface and width, as in a centered dialog.
 */
@Composable
fun FoldDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    placement: DialogPlacement = rememberDialogPlacement(),
    content: @Composable () -> Unit,
) {
    val isInHalf = placement != DialogPlacement.Centered
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = if (isInHalf) halfPanePropertiesOf(properties) else properties,
    ) {
        if (isInHalf) {
            FoldHalfPane(
                placement = placement,
                onScrimClick = onDismissRequest.takeIf { properties.dismissOnClickOutside },
                content = content,
            )
        } else {
            content()
        }
    }
}

// Why: the window takes the whole screen, so "outside" is the scrim the pane draws, not the platform's.
private fun halfPanePropertiesOf(properties: DialogProperties): DialogProperties = DialogProperties(
    dismissOnBackPress = properties.dismissOnBackPress,
    dismissOnClickOutside = false,
    securePolicy = properties.securePolicy,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = properties.decorFitsSystemWindows,
)

/*
 * Why: the content blocks the taps on it (an empty pointerInput, as Material's Surface), so only the scrim around
 * it dismisses.
 */
@Composable
private fun FoldHalfPane(
    placement: DialogPlacement,
    onScrimClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val fold = rememberFoldLayout()
    val isTopHalf = placement == DialogPlacement.TopHalf
    val split = if (isTopHalf) fold.tabletopSplit else fold.bookSplit
    var originOnScreen by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxSize()) {
        if (onScrimClick != null) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(onScrimClick) { detectTapGestures { onScrimClick() } },
            )
        }
        Layout(
            content = {
                Box(modifier = Modifier.pointerInput(Unit) {}) { content() }
            },
            modifier = Modifier
                .fillMaxSize()
                .onPlaced { coordinates -> originOnScreen = coordinates.positionOnScreen() },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val origin = if (isTopHalf) originOnScreen.y else originOnScreen.x
            val gapRange = halvesGapOf(
                length = if (isTopHalf) height else width,
                band = split?.let { it.bandStart.toPx() - origin..it.bandEnd.toPx() - origin },
                gap = HingeBand.roundToPx(),
            )
            val half = when {
                isTopHalf -> IntRect(
                    left = 0,
                    top = 0,
                    right = width,
                    bottom = gapRange.first,
                )

                layoutDirection == LayoutDirection.Ltr -> IntRect(
                    left = gapRange.last,
                    top = 0,
                    right = width,
                    bottom = height,
                )

                else -> IntRect(
                    left = 0,
                    top = 0,
                    right = gapRange.first,
                    bottom = height,
                )
            }
            val margin = halfPaneMargin.roundToPx()
            val placeable = measurables.single().measure(
                Constraints(
                    maxWidth = (half.width - 2 * margin).coerceAtLeast(0),
                    maxHeight = (half.height - 2 * margin).coerceAtLeast(0),
                ),
            )
            layout(width, height) {
                placeable.place(
                    x = half.left + (half.width - placeable.width) / 2,
                    y = half.top + (half.height - placeable.height) / 2,
                )
            }
        }
    }
}

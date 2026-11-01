package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val halfPaneMargin = 16.dp
private val alertDialogMinWidth = 280.dp
private val alertDialogMaxWidth = 560.dp

/*
 * Why: a dialog that keeps off the hinge. Centered it is a plain Dialog, as before; in a half its window covers the
 * screen, so the platform's scrim dims both halves, and the content sits centered in its half, at most the half minus
 * 16dp each side (the canvas' book dialog). On a tabletop lying down the end half stops above the hinge across it,
 * so nothing crosses either. Tapping outside it still dismisses, when the properties say so. The content brings its
 * own surface and width, as in a centered dialog.
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

/*
 * Why: what Material's BasicAlertDialog gives its content, for a FoldDialog that replaces one: 280 to 560dp wide, and
 * the pane title TalkBack says as the dialog opens, here the dialog's own title. A wider dialog (a bill's) skips it.
 */
fun Modifier.alertDialogPane(title: String): Modifier = sizeIn(
    minWidth = alertDialogMinWidth,
    maxWidth = alertDialogMaxWidth,
).semantics { paneTitle = title }

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
    val hingeGap = rememberLayoutHingeGap()
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
                .then(hingeGap.modifier),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val topHalfBottom = hingeGap
                .gapOf(
                    density = this,
                    split = fold.tabletopSplit,
                    isSideBySide = false,
                    length = height,
                ).first
            val half = if (placement == DialogPlacement.TopHalf) {
                IntRect(
                    left = 0,
                    top = 0,
                    right = width,
                    bottom = topHalfBottom,
                )
            } else {
                endHalfOf(
                    sideGap = hingeGap.gapOf(
                        density = this,
                        split = fold.bookSplit,
                        isSideBySide = true,
                        length = width,
                    ),
                    width = width,
                    // Why: a tabletop lying down has the book's halves and its own hinge across them too.
                    bottom = if (fold.tabletopSplit != null) topHalfBottom else height,
                    layoutDirection = layoutDirection,
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

// Why: the half after the hinge (right in English and Portuguese), down to `bottom`.
private fun endHalfOf(
    sideGap: IntRange,
    width: Int,
    bottom: Int,
    layoutDirection: LayoutDirection,
): IntRect = if (layoutDirection == LayoutDirection.Ltr) {
    IntRect(
        left = sideGap.last,
        top = 0,
        right = width,
        bottom = bottom,
    )
} else {
    IntRect(
        left = 0,
        top = 0,
        right = sideGap.first,
        bottom = bottom,
    )
}

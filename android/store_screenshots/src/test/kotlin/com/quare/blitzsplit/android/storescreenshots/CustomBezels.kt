package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Each frame takes the width the canvas offers and its height from the screen's own proportions, never a fixed size:
// the library measures a custom frame at the offered width, so a fixed size wider than that is narrowed alone and the
// capture is drawn squeezed (ScaleToFit then shrinks a frame too tall for the canvas, both ways alike).
private const val DUO_ASPECT_RATIO = 1398f / 2034f
private const val DUO_OPEN_ASPECT_RATIO = 2853f / 2007f
private const val MAC_WINDOW_ASPECT_RATIO = 1440f / 900f
private val duoBezel = 18.dp
private val duoCorner = 44.dp
private val macCorner = 16.dp

/** The iPhone Duo closed, its outer display drawn in a plain bezel: store-screenshots has no Duo frame. */
@Composable
internal fun DuoBezelSurface(content: @Composable () -> Unit) {
    DuoBezelBox(
        aspectRatio = DUO_ASPECT_RATIO,
        content = content,
    )
}

/** The iPhone Duo open and lying down, its inner display in the closed one's bezel. */
@Composable
internal fun DuoOpenBezelSurface(content: @Composable () -> Unit) {
    DuoBezelBox(
        aspectRatio = DUO_OPEN_ASPECT_RATIO,
        content = content,
    )
}

@Composable
private fun DuoBezelBox(
    aspectRatio: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(duoCorner + duoBezel),
            ).background(Color.Black, RoundedCornerShape(duoCorner + duoBezel))
            .border(2.dp, Color(0xFF3A3A3C), RoundedCornerShape(duoCorner + duoBezel))
            .padding(duoBezel),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(RoundedCornerShape(duoCorner))) {
            content()
        }
    }
}

/** The Mac app's window, its own title bar included in the capture, floating with the system's shadow. */
@Composable
internal fun MacWindowSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(macCorner),
            ).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(macCorner))
            .fillMaxWidth()
            .aspectRatio(MAC_WINDOW_ASPECT_RATIO)
            .clip(RoundedCornerShape(macCorner)),
    ) {
        content()
    }
}

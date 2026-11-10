package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The Duo closed, its outer display (1398x2034 px) at 2x in a black bezel, and the Mac window at its captured 1440x900
// points.
private val duoScreenWidth = 699.dp
private val duoScreenHeight = 1017.dp
private val duoBezel = 18.dp
private val duoCorner = 44.dp
private val macWindowWidth = 1440.dp
private val macWindowHeight = 900.dp
private val macCorner = 16.dp

/** The iPhone Duo closed, drawn as a plain bezel: store-screenshots has no Duo frame. */
@Composable
internal fun DuoBezelSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(duoCorner + duoBezel),
            ).background(Color.Black, RoundedCornerShape(duoCorner + duoBezel))
            .border(2.dp, Color(0xFF3A3A3C), RoundedCornerShape(duoCorner + duoBezel))
            .padding(duoBezel),
    ) {
        Box(modifier = Modifier.size(duoScreenWidth, duoScreenHeight).clip(RoundedCornerShape(duoCorner))) {
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
            .size(macWindowWidth, macWindowHeight)
            .clip(RoundedCornerShape(macCorner)),
    ) {
        content()
    }
}

package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lucianosantos.storescreenshots.DeviceMockup
import dev.lucianosantos.storescreenshots.FormFactor
import dev.lucianosantos.storescreenshots.MockupOrientation

// The time on every status bar a frame draws, Play's phone and tablets: the one Apple's own screenshots show.
internal const val STATUS_BAR_CLOCK = "9:41"

// Each frame takes the width the canvas offers and its height from the screen's own proportions, never a fixed size:
// the library measures a custom frame at the offered width, so a fixed size wider than that is narrowed alone and the
// capture is drawn squeezed (ScaleToFit then shrinks a frame too tall for the canvas, both ways alike).
//
// The IPAD_ values are the iPad Air 13" lying down, in the device's own points: what store-screenshots measured off
// the simulator for its upright iPad frame (IPadAir13Metrics, which the library keeps to itself and only draws
// upright), turned a quarter to the left. That side puts the long edge with the front camera on top, as on a keyboard,
// the volume buttons on the top edge by the left corner and the top button on the left edge: the way the UI test holds
// the iPad (landscapeLeft).
private const val DUO_ASPECT_RATIO = 1398f / 2034f
private const val DUO_OPEN_ASPECT_RATIO = 2853f / 2007f
private const val MAC_WINDOW_ASPECT_RATIO = 1440f / 900f
private const val IPAD_BODY_WIDTH = 1484f
private const val IPAD_BODY_HEIGHT = 1138.4f
private const val IPAD_SCREEN_WIDTH = 1366f
private const val IPAD_SCREEN_HEIGHT = 1024f
private const val IPAD_BODY_CORNER = 83f
private const val IPAD_SCREEN_CORNER = 12f
private const val IPAD_RAIL = 5.25f
private const val IPAD_RIM = 1.2f
private const val IPAD_BUTTON_CORNER = 1.8f
private const val IPAD_VOLUME_PROTRUSION = 2.3f
private const val IPAD_TOP_BUTTON_PROTRUSION = 1.2f
private const val IPAD_TOP_BUTTON_TOP = 85.2f
private const val IPAD_TOP_BUTTON_HEIGHT = 85.3f
private const val IPAD_CAMERA_DIAMETER = 7f
private const val IPAD_CAMERA_LENS_DIAMETER = 3f
private val duoBezel = 18.dp
private val duoCorner = 44.dp
private val macCorner = 16.dp
private val iPadRimColor = Color(0xFF5B5B5B)
private val iPadRailColor = Color(0xFF272727)
private val iPadBezelColor = Color(0xFF020202)
private val iPadCameraColor = Color(0xFF0E0F13)
private val iPadCameraLensColor = Color(0xFF1B2130)
private val iPadVolumeButtons = listOf(123.8f to 47.9f, 185.7f to 49f)

/**
 * A Play tablet lying down: the library's own tablet, turned a quarter. Its tablet frame only stands upright, but the
 * mockup that frame draws takes an orientation, and lying down it measures the screen wider than tall and tells it
 * so (the window's size and the configuration's orientation), which is what makes a screen pick its layout for a
 * tablet lying down. The status bar is the one the upright frame drew, along the top edge, the screen under it.
 */
@Composable
internal fun PlayTabletLandscapeSurface(
    formFactor: FormFactor,
    content: @Composable () -> Unit,
) {
    DeviceMockup(
        formFactor = formFactor,
        orientation = MockupOrientation.Landscape,
        statusBarClock = STATUS_BAR_CLOCK,
        edgeToEdge = false,
        content = content,
    )
}

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

/**
 * The 13" iPad lying down, around the capture of the iPad's slot: the body's rim, rail and black bezel of the library's
 * upright iPad frame, with the front camera in the top bezel. Every measure is a fraction of the width offered, so the
 * iPad keeps its proportions at any size and the capture is never stretched.
 */
@Composable
internal fun IPadLandscapeBezelSurface(content: @Composable () -> Unit) {
    BoxWithConstraints {
        // One of the iPad's points, in this frame's dp.
        val point = maxWidth / IPAD_BODY_WIDTH
        // Drawn first, so the body covers all but what stands proud of its edge.
        IPadLandscapeButtonsLayout(point)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IPAD_BODY_WIDTH / IPAD_BODY_HEIGHT)
                .background(iPadRimColor, RoundedCornerShape(point * IPAD_BODY_CORNER))
                .padding(point * IPAD_RIM)
                .background(iPadRailColor, RoundedCornerShape(point * (IPAD_BODY_CORNER - IPAD_RIM)))
                .padding(point * (IPAD_RAIL - IPAD_RIM))
                .background(iPadBezelColor, RoundedCornerShape(point * (IPAD_BODY_CORNER - IPAD_RAIL))),
        ) {
            IPadFrontCameraBox(point)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        width = point * IPAD_SCREEN_WIDTH,
                        height = point * IPAD_SCREEN_HEIGHT,
                    ).clip(RoundedCornerShape(point * IPAD_SCREEN_CORNER)),
            ) {
                content()
            }
        }
    }
}

// The front camera, in the middle of the bezel above the screen: barely lighter than the bezel, as on the device.
@Composable
private fun BoxScope.IPadFrontCameraBox(point: Dp) {
    val bezel = point * ((IPAD_BODY_HEIGHT - IPAD_SCREEN_HEIGHT) / 2 - IPAD_RAIL)
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = (bezel - point * IPAD_CAMERA_DIAMETER) / 2)
            .size(point * IPAD_CAMERA_DIAMETER)
            .background(iPadCameraColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(point * IPAD_CAMERA_LENS_DIAMETER).background(iPadCameraLensColor, CircleShape))
    }
}

// The volume buttons on the top edge (iPadVolumeButtons: where each starts, from the body's left, and how long it is)
// and the top button on the left edge, each as deep into the body as it stands proud of it, so its rounded end is the
// only part seen.
@Composable
private fun BoxScope.IPadLandscapeButtonsLayout(point: Dp) {
    val corner = point * IPAD_BUTTON_CORNER
    iPadVolumeButtons.forEach { (left, width) ->
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = point * left,
                    y = -(point * IPAD_VOLUME_PROTRUSION),
                ).size(
                    width = point * width,
                    height = point * (IPAD_VOLUME_PROTRUSION * 2),
                ).background(
                    color = iPadRailColor,
                    shape = RoundedCornerShape(
                        topStart = corner,
                        topEnd = corner,
                    ),
                ),
        )
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(
                x = -(point * IPAD_TOP_BUTTON_PROTRUSION),
                y = point * IPAD_TOP_BUTTON_TOP,
            ).size(
                width = point * (IPAD_TOP_BUTTON_PROTRUSION * 2),
                height = point * IPAD_TOP_BUTTON_HEIGHT,
            ).background(
                color = iPadRailColor,
                shape = RoundedCornerShape(
                    topStart = corner,
                    bottomStart = corner,
                ),
            ),
    )
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

package com.quare.blitzsplit.android.designsystem.layout

import android.content.Context
import androidx.compose.material3.adaptive.collectFoldingFeaturesAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator

// Why: the canvas' hinge-gap, the band nothing crosses on a book or a tabletop, whatever width the hinge reports.
val HingeBand: Dp = 24.dp

internal val TabletSmallestWidth: Dp = 600.dp

// Why: a foldable's inner screen is under 960dp on its longest side, a tablet's is not.
internal val TabletLongestSide: Dp = 960.dp

// Why: a Flip's cover screen (the canvas' 360×374, a Razr's about 410) is smaller than any phone's whole screen.
private val coverScreenLongestSide: Dp = 480.dp

/*
 * Why: the size the canvas draws a screen at, and where its halves meet, from the app's window and the fold the device
 * reports in it (decision 8: the hinge, not the window alone). A window that reports no fold falls back on its size:
 * a foldable-sized one lying down (an 841×673 emulator) splits in the middle like a book.
 */
@Immutable
data class FoldLayout(
    // Why: on screen, as the hinge, so a dialog's own window can line up with both.
    val windowBounds: DpRect,
    val hinge: FoldHinge?,
    /*
     * Why: the app runs on a Flip's cover screen, where Grupos only draws its cover (5.1): the whole display is that
     * small, not the window alone, so a phone's split screen half never counts as one.
     */
    val isOnCoverScreen: Boolean,
) {
    val formFactor: FormFactor = formFactorOf(
        width = windowBounds.width,
        height = windowBounds.height,
        hinge = hinge,
    )

    /*
     * Why: the halves side by side. A book's meet on its vertical hinge (or in the middle without one); a tabletop
     * lying down has them too, since the canvas draws its screens like the book's where they have no top and bottom.
     */
    val bookSplit: HingeSplit? = when (formFactor) {
        FormFactor.Book -> if (hinge?.isVertical == true) {
            bandOf(hinge.bounds.left, hinge.bounds.right)
        } else {
            bandOf(middleOf(windowBounds.left, windowBounds.right))
        }

        FormFactor.Tabletop -> if (windowBounds.width > windowBounds.height) {
            bandOf(middleOf(windowBounds.left, windowBounds.right))
        } else {
            null
        }

        else -> null
    }

    // Why: a tabletop's top and bottom, on its half opened horizontal hinge.
    val tabletopSplit: HingeSplit? = if (formFactor == FormFactor.Tabletop && hinge != null) {
        bandOf(hinge.bounds.top, hinge.bounds.bottom)
    } else {
        null
    }
}

/*
 * Why: read once at the root and handed down, so a dialog's window (its own LocalWindowInfo) still sees the activity's
 * window and fold. Null outside the root: a preview reads its own window instead.
 */
val LocalFoldLayout = compositionLocalOf<FoldLayout?> { null }

@Composable
fun FoldLayoutRoot(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFoldLayout provides rememberWindowFoldLayout(), content = content)
}

@Composable
fun rememberFoldLayout(): FoldLayout = LocalFoldLayout.current ?: rememberWindowFoldLayout()

// Why: a cover screen preview (the canvas' Flip, 360×374), since a preview's display is its window.
fun coverFoldLayoutOf(
    width: Dp,
    height: Dp,
): FoldLayout = FoldLayout(
    windowBounds = DpRect(
        left = 0.dp,
        top = 0.dp,
        right = width,
        bottom = height,
    ),
    hinge = null,
    isOnCoverScreen = true,
)

// Why: a tabletop preview, since a preview's window reports no fold: the hinge across the middle, half opened.
fun tabletopFoldLayoutOf(
    width: Dp,
    height: Dp,
): FoldLayout = FoldLayout(
    windowBounds = DpRect(
        left = 0.dp,
        top = 0.dp,
        right = width,
        bottom = height,
    ),
    hinge = FoldHinge(
        bounds = DpRect(
            left = 0.dp,
            top = height / 2,
            right = width,
            bottom = height / 2,
        ),
        isVertical = false,
        isFlat = false,
        isSeparating = true,
    ),
    isOnCoverScreen = false,
)

/*
 * Why: the origin is read again whenever the configuration changes, which a window moved without resizing
 * (freeform, desktop windowing, a split screen window moved) brings while containerSize stays, and is itself a key,
 * so the bounds never keep the old place on screen.
 */
@Composable
private fun rememberWindowFoldLayout(): FoldLayout {
    val foldingFeatures by collectFoldingFeaturesAsState()
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val origin = remember(context, configuration, containerSize, foldingFeatures) { windowOriginOf(context) }
    // Why: the whole display, which moving between a Flip's cover and its inner screen changes with the configuration.
    val displaySize = remember(context, configuration, containerSize) { displaySizeOf(context) ?: containerSize }
    return remember(foldingFeatures, containerSize, density, origin, displaySize) {
        density.foldLayoutOf(
            origin = origin,
            size = containerSize,
            feature = foldingFeatures.firstOrNull(),
            isOnCoverScreen = density.isCoverScreen(displaySize),
        )
    }
}

// Why: the largest window the display allows, its whole size; null where it can't be read (a preview).
private fun displaySizeOf(context: Context): IntSize? = runCatching {
    val bounds = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context).bounds
    IntSize(bounds.width(), bounds.height())
}.getOrNull()?.takeIf { it.width > 0 && it.height > 0 }

private fun Density.isCoverScreen(displaySize: IntSize): Boolean =
    maxOf(displaySize.width, displaySize.height).toDp() < coverScreenLongestSide

// Why: a preview has no window to measure; it starts at the screen's corner, as a full screen app does.
private fun windowOriginOf(context: Context): IntOffset = runCatching {
    val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context).bounds
    IntOffset(bounds.left, bounds.top)
}.getOrDefault(IntOffset.Zero)

private fun Density.foldLayoutOf(
    origin: IntOffset,
    size: IntSize,
    feature: FoldingFeature?,
    isOnCoverScreen: Boolean,
): FoldLayout = FoldLayout(
    windowBounds = DpRect(
        left = origin.x.toDp(),
        top = origin.y.toDp(),
        right = (origin.x + size.width).toDp(),
        bottom = (origin.y + size.height).toDp(),
    ),
    hinge = feature?.let {
        FoldHinge(
            bounds = DpRect(
                left = (origin.x + it.bounds.left).toDp(),
                top = (origin.y + it.bounds.top).toDp(),
                right = (origin.x + it.bounds.right).toDp(),
                bottom = (origin.y + it.bounds.bottom).toDp(),
            ),
            isVertical = it.orientation == FoldingFeature.Orientation.VERTICAL,
            isFlat = it.state == FoldingFeature.State.FLAT,
            isSeparating = it.isSeparating,
        )
    },
    isOnCoverScreen = isOnCoverScreen,
)

/*
 * Why: a phone stays a phone whatever it reports (a Flip open is one, as the canvas says). On anything larger, a
 * vertical hinge makes a book in any posture and a half opened horizontal one a tabletop; a flat horizontal one, or a
 * window under 960dp without a fold, is a foldable: upright, or a book lying down.
 */
private fun formFactorOf(
    width: Dp,
    height: Dp,
    hinge: FoldHinge?,
): FormFactor {
    val isLyingDown = width > height
    return when {
        min(width, height) < TabletSmallestWidth -> if (isLyingDown) FormFactor.PhoneLandscape else FormFactor.Phone

        hinge?.isVertical == true -> FormFactor.Book

        hinge?.isFlat == false -> FormFactor.Tabletop

        hinge == null && max(width, height) >= TabletLongestSide ->
            if (isLyingDown) FormFactor.TabletLandscape else FormFactor.TabletPortrait

        isLyingDown -> FormFactor.Book

        else -> FormFactor.FoldableUpright
    }
}

private fun middleOf(
    start: Dp,
    end: Dp,
): Dp = (start + end) / 2

private fun bandOf(center: Dp): HingeSplit = bandOf(
    start = center,
    end = center,
)

private fun bandOf(
    start: Dp,
    end: Dp,
): HingeSplit {
    val center = middleOf(start, end)
    val halfBand = max(HingeBand, end - start) / 2
    return HingeSplit(
        bandStart = center - halfBand,
        bandEnd = center + halfBand,
    )
}

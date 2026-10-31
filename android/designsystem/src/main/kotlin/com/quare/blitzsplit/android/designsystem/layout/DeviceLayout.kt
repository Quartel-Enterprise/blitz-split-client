package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min

private val tabletSmallestWidth = 600.dp
private val formDialogLongestSide = 960.dp

/*
 * Why: the canvas draws every screen four times (phone and tablet, each upright and lying down).
 * Lying down, every screen splits into two columns; a tablet swaps the bottom bar for a navigation
 * rail and the recovery code sheet for a dialog.
 */
@Immutable
data class DeviceLayout(
    val isTablet: Boolean,
    val isLandscape: Boolean,
) {
    val usesTwoColumns: Boolean
        get() = isLandscape

    // Why: the bottom bar only fits a phone held upright; anything wider gets the rail.
    val usesNavigationRail: Boolean
        get() = isTablet || isLandscape

    companion object {
        val PhonePortrait: DeviceLayout = DeviceLayout(
            isTablet = false,
            isLandscape = false,
        )
        val PhoneLandscape: DeviceLayout = DeviceLayout(
            isTablet = false,
            isLandscape = true,
        )
        val TabletPortrait: DeviceLayout = DeviceLayout(
            isTablet = true,
            isLandscape = false,
        )
        val TabletLandscape: DeviceLayout = DeviceLayout(
            isTablet = true,
            isLandscape = true,
        )
    }
}

private fun deviceLayoutOf(
    width: Dp,
    height: Dp,
): DeviceLayout = DeviceLayout(
    isTablet = min(width, height) >= tabletSmallestWidth,
    isLandscape = width > height,
)

/*
 * Why: the layout follows the window, never the space left under the keyboard. Measured after
 * imePadding, a tablet lying down would drop under 600dp of height as the keyboard opens, turn into
 * the phone layout, rebuild the focused field and close the keyboard again.
 */
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable (DeviceLayout) -> Unit,
) {
    val layout = rememberDeviceLayout()
    Box(modifier = modifier) { content(layout) }
}

/*
 * Why: the canvas opens a form like a new payment key as a 560dp dialog on a tablet, but full screen on an
 * unfolded foldable, whose inner screen (under 960dp on its longest side) a centered dialog would cross at
 * the hinge.
 */
@Composable
fun rememberUsesFormDialog(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) {
        with(density) {
            val width = size.width.toDp()
            val height = size.height.toDp()
            min(width, height) >= tabletSmallestWidth && max(width, height) >= formDialogLongestSide
        }
    }
}

/*
 * Why: the canvas draws what opens over a group (Rachar conta, Conta, Registrar pagamento) as a dialog on a tablet
 * and on the upright foldable, and full screen (or a sheet) on a phone, lying down and on the tabletop (a foldable
 * lying down, under 960dp wide), unlike the other forms, which open full screen on any foldable.
 */
@Composable
fun rememberUsesGroupDialog(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) {
        with(density) {
            val width = size.width.toDp()
            val height = size.height.toDp()
            val isTablet = min(width, height) >= tabletSmallestWidth
            isTablet && (height > width || max(width, height) >= formDialogLongestSide)
        }
    }
}

@Composable
fun rememberDeviceLayout(): DeviceLayout {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) {
        with(density) {
            deviceLayoutOf(
                width = size.width.toDp(),
                height = size.height.toDp(),
            )
        }
    }
}

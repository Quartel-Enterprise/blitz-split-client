package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width

private val formDialogFormFactors = setOf(FormFactor.TabletPortrait, FormFactor.TabletLandscape)
private val groupDialogFormFactors = formDialogFormFactors + FormFactor.FoldableUpright

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
    isTablet = min(width, height) >= TabletSmallestWidth,
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
 * Why: the canvas opens a form like a new payment key as a 560dp dialog on a tablet, but full screen on any
 * foldable, which a centered dialog would cross at the hinge. The foldable is the one the device reports (decision
 * 8), or a window under 960dp on its longest side that reports none.
 */
@Composable
fun rememberUsesFormDialog(): Boolean = rememberFoldLayout().formFactor in formDialogFormFactors

/*
 * Why: the canvas draws what opens over a group (Rachar conta, Conta, Registrar pagamento) as a dialog on a tablet
 * and on the foldable upright (its hinge across and flat), and full screen (or a sheet) on a phone, lying down, on a
 * book and on a tabletop, unlike the other forms, which open full screen on any foldable. The book's dialog in the
 * end half (FoldDialog) is each flow's to adopt.
 */
@Composable
fun rememberUsesGroupDialog(): Boolean = rememberFoldLayout().formFactor in groupDialogFormFactors

/*
 * Why: the activity's window, the one FoldLayout reads, so a screen inside a dialog's window still sizes itself by
 * what it opened over and agrees with rememberFoldLayout() on whether it is a tablet. In the pane beside the home's
 * list, a group's screen takes the one column of a tablet upright.
 */
@Composable
fun rememberDeviceLayout(): DeviceLayout {
    val window = rememberFoldLayout().windowBounds
    val isInDetailPane = LocalInDetailPane.current
    return remember(window, isInDetailPane) {
        if (isInDetailPane) {
            DeviceLayout.TabletPortrait
        } else {
            deviceLayoutOf(
                width = window.width,
                height = window.height,
            )
        }
    }
}

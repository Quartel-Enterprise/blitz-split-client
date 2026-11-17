package com.quare.blitzsplit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry

/*
 * Why: whether the scene with this key is drawn as a back gesture's preview rather than as an overlay: a gesture is in
 * progress and the scene is not among the overlays NavDisplay draws (the sheet really open, or one opening, is never
 * the preview: its covered screen would be drawn twice). While the gesture swipes the top screen away, Navigation 3
 * draws the scene under it inside its animated content, which for a sheet or a dialog opens a window mid-gesture, and
 * the system takes that for the end of the gesture: it cancels it, so back over a profile opened from "Did Larissa pay
 * you?" never left the profile (beta feedback of 2026-11-16).
 */
internal val LocalIsBackPreview = compositionLocalOf<(Any) -> Boolean> { { false } }

/*
 * Why: the preview of a sheet's or a dialog's scene is the screen it covers, as it will be seen once the sheet is
 * drawn over it; the sheet or the dialog itself opens only once the scene is one of the overlays NavDisplay draws.
 */
@Composable
internal fun <T : Any> OverlayOrPreviewComponent(
    entry: NavEntry<T>,
    overlaidEntries: List<NavEntry<T>>,
    overlay: @Composable () -> Unit,
) {
    if (LocalIsBackPreview.current(entry.contentKey)) {
        overlaidEntries.lastOrNull()?.Content()
    } else {
        overlay()
    }
}

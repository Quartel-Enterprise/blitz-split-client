package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Why: the app's bottom sheet scene closes on its own (a drag down, the scrim, the back gesture), before the screen in
 * it hears about it; the screen holds it open through this while it must not close, a payment being saved.
 */
val LocalSheetDismissLock = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/*
 * Why: what the sheet does when a drag down or the scrim asks it to close while it is held open: a form with unsaved
 * input asks "Discard changes?" there, so the dismissal is answered instead of silently ignored.
 */
val LocalSheetBlockedDismiss = staticCompositionLocalOf<MutableState<(() -> Unit)?>?> { null }

// Why: outside a bottom sheet (full screen, a dialog), there is nothing to hold.
@Composable
fun SheetDismissLockEffect(
    isLocked: Boolean,
    onBlockedDismiss: (() -> Unit)? = null,
) {
    val lock = LocalSheetDismissLock.current ?: return
    val blocked = LocalSheetBlockedDismiss.current
    val latestOnBlockedDismiss by rememberUpdatedState(onBlockedDismiss)
    DisposableEffect(lock, isLocked) {
        lock.value = isLocked
        blocked?.value = { latestOnBlockedDismiss?.invoke() }
        onDispose {
            lock.value = false
            blocked?.value = null
        }
    }
}

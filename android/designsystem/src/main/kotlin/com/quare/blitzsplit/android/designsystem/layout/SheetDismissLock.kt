package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Why: the app's bottom sheet scene closes on its own (a drag down, the scrim, the back gesture), before the screen in
 * it hears about it; the screen holds it open through this while it must not close, a payment being saved.
 */
val LocalSheetDismissLock = staticCompositionLocalOf<MutableState<Boolean>?> { null }

// Why: outside a bottom sheet (full screen, a dialog), there is nothing to hold.
@Composable
fun SheetDismissLockEffect(isLocked: Boolean) {
    val lock = LocalSheetDismissLock.current ?: return
    DisposableEffect(lock, isLocked) {
        lock.value = isLocked
        onDispose { lock.value = false }
    }
}

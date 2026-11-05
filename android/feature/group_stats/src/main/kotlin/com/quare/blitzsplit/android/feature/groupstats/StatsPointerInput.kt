package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/*
 * Why: a mouse or trackpad picks a bar or a slice by hovering it and lets go when it leaves; touch never hovers. The
 * callbacks must only write state that outlives the gesture, since they stay the ones of the first composition until
 * the key changes.
 */
internal fun Modifier.onMouseHover(
    key: Any?,
    onMove: PointerInputScope.(position: Offset) -> Unit,
    onExit: () -> Unit,
): Modifier = pointerInput(key) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            if (change.type != PointerType.Mouse) continue
            when (event.type) {
                PointerEventType.Move, PointerEventType.Enter -> this@pointerInput.onMove(change.position)
                PointerEventType.Exit -> onExit()
            }
        }
    }
}

/*
 * Why: a tap that says whether a mouse made it, so a click keeps what hovering already picked while a second tap of a
 * finger lets it go. A drag that another detector consumes is no tap.
 */
internal suspend fun PointerInputScope.detectPickTaps(onTap: (position: Offset, isMouse: Boolean) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
        onTap(up.position, down.type == PointerType.Mouse)
    }
}

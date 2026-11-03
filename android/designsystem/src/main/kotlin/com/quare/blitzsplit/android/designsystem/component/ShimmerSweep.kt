package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Why: one sweep per loading region, read by every block in it while drawing, so the blocks shimmer in step and a
 * frame of the sweep only redraws them, never recomposes. A null progress is the static placeholder (reduce motion).
 */
@Stable
internal class ShimmerSweep(
    val progress: State<Float>?,
    val baseColor: Color,
    val highlightColor: Color,
)

// Why: a block drawn outside a ShimmerBox still shows its shape, static and in a neutral grey.
internal val LocalShimmerSweep = staticCompositionLocalOf {
    ShimmerSweep(
        progress = null,
        baseColor = Color.Gray.copy(alpha = 0.2f),
        highlightColor = Color.Gray.copy(alpha = 0.2f),
    )
}

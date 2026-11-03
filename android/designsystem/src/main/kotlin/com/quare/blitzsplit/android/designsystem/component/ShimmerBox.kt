package com.quare.blitzsplit.android.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import com.quare.blitzsplit.android.designsystem.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val shimmerAppearDelay = 150.milliseconds
private val shimmerSweepDuration = 1300.milliseconds
private val shimmerFadeInDuration = 200.milliseconds

/*
 * Why: a region still loading (Pierre's call: never a guess while loading). It shows only after 150 ms, fading in, so
 * a fast read of the device never flashes; until then its slot draws nothing. Its blocks (Modifier.shimmer and the
 * Shimmer* pieces) sweep together, static under reduce motion (the animator scale at 0), and the screen reader hears
 * one "Carregando" for the whole region rather than each block.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isDelayed: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val isInspecting = LocalInspectionMode.current
    var isShown by remember { mutableStateOf(!isDelayed || isInspecting) }
    LaunchedEffect(Unit) {
        if (!isShown) {
            delay(shimmerAppearDelay)
            isShown = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        animationSpec = tween(shimmerFadeInDuration.inWholeMilliseconds.toInt()),
        label = "shimmerAlpha",
    )
    val loadingLabel = stringResource(R.string.designsystem_loading)
    val progress = rememberShimmerProgress(isAnimated = !rememberIsReducedMotion() && !isInspecting)
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlightColor = MaterialTheme.colorScheme.surfaceBright
    val sweep = remember(progress, baseColor, highlightColor) {
        ShimmerSweep(
            progress = progress,
            baseColor = baseColor,
            highlightColor = highlightColor,
        )
    }
    CompositionLocalProvider(LocalShimmerSweep provides sweep) {
        Box(
            modifier = modifier
                .graphicsLayer { this.alpha = alpha }
                .clearAndSetSemantics {
                    if (isShown) {
                        contentDescription = loadingLabel
                        liveRegion = LiveRegionMode.Polite
                    }
                },
            content = content,
        )
    }
}

@Composable
private fun rememberShimmerProgress(isAnimated: Boolean): State<Float>? {
    if (!isAnimated) return null
    val transition = rememberInfiniteTransition(label = "shimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = shimmerSweepDuration.inWholeMilliseconds.toInt(),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
}

// Why: Android's "Remove animations" sets the animator scale to 0; the placeholder then stays still.
@Composable
private fun rememberIsReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

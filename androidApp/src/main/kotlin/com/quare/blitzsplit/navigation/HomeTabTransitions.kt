package com.quare.blitzsplit.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene

// Why: the web's page-enter, so a tab's content comes in as fast on every platform.
private const val HOME_TAB_FADE_MILLIS = 180

private object HomeTabKey : NavMetadataKey<Boolean>

// Why: the home's tabs (Início, Grupos, Saldos, Contatos and Buscar) draw the same bars, which a switch keeps still.
internal val homeTabMetadata: Map<String, Any> = metadata { put(HomeTabKey, true) }

// Why: any of its entries, so a tablet's Groups beside the group open over it still counts as the tab.
private val Scene<*>.isHomeTab: Boolean
    get() = entries.any { entry -> entry.metadata[HomeTabKey] == true }

/*
 * Why: each tab draws its own rail or bottom bar, the same in every tab, so crossfading the two whole screens dimmed the
 * bars with the content, for 700 ms. Between two tabs one screen stays opaque under the other while it fades, so the
 * bars hold still and only the content changes: going forward, the screen coming in fades in over the one leaving;
 * going back, which draws the tab coming back underneath, the one leaving fades out over it. Anything else keeps
 * Navigation 3's default.
 */
internal fun <T : Any> createHomeTabTransitionSpec(
    isPop: Boolean,
    default: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    if (isBetweenHomeTabs()) createHomeTabFade(isPop) else default()
}

// Why: the back gesture between two tabs fades the tab leaving as it follows the finger, instead of shrinking its bars.
internal fun <T : Any> createHomeTabPredictivePopTransitionSpec(
    default: AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform,
): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = { swipeEdge ->
    if (isBetweenHomeTabs()) createHomeTabFade(isPop = true) else default(swipeEdge)
}

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.isBetweenHomeTabs(): Boolean =
    initialState.isHomeTab && targetState.isHomeTab

private fun createHomeTabFade(isPop: Boolean): ContentTransform = if (isPop) {
    EnterTransition.None togetherWith fadeOut(tween(HOME_TAB_FADE_MILLIS))
} else {
    fadeIn(tween(HOME_TAB_FADE_MILLIS)) togetherWith ExitTransition.KeepUntilTransitionsFinished
}

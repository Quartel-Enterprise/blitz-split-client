package com.quare.blitzsplit.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.isGroupTabSwitch

/*
 * Why: how long the tab left stays drawn under the next one while that one reads the group, NavDisplay's own
 * duration. Anything but a tab switch gets null, which falls back to NavDisplay's own transition.
 */
private const val TAB_LEFT_HOLD_MILLIS = 700
private val groupTabTransition: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform? = {
    val from = initialState.metadata[GroupTabRouteKey]
    val to = targetState.metadata[GroupTabRouteKey]
    val isTabSwitch = from != null && to != null && isGroupTabSwitch(from, to)
    if (isTabSwitch) EnterTransition.None togetherWith fadeOut(snap(delayMillis = TAB_LEFT_HOLD_MILLIS)) else null
}

/*
 * Why: Despesas, Saldos and Membros take each other's place on the back stack, but they are tabs of one screen: moving
 * between them swaps the content in place, the group's header still, instead of NavDisplay's crossfade of a whole
 * screen: the next tab shows at once, see-through over the one left until it has read the group
 * (`groupTabContainerColorOf`). Opening or leaving the group keeps the crossfade. Beside the list the scene stays the
 * same, so nothing plays there.
 */
internal fun groupTabMetadataOf(route: Route): Map<String, Any> = homeDetailPaneMetadata +
    NavDisplay.transitionSpec(groupTabTransition) +
    NavDisplay.popTransitionSpec(groupTabTransition) +
    metadata { put(GroupTabRouteKey, route) }

private object GroupTabRouteKey : NavMetadataKey<Route>

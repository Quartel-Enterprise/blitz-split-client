package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/*
 * Why: the navigation's shared transition scope, null wherever the group's row and the group's screen can't swap
 * places in one transition: anywhere the group opens beside the list, in previews and in tests.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

// Why: the transition of the back stack entry a screen draws in, which the shared elements follow, back gesture too.
val LocalEntryVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

// Why: one key per group and element, so only the same group's avatar and name fly from its row to its screen.
private data class GroupSharedKey(
    val groupId: String,
    val element: GroupSharedElement,
)

private enum class GroupSharedElement { AVATAR, NAME }

// Why: the modifiers a group's avatar and name take to fly between its row or card and its screen's header.
@Immutable
class GroupSharedModifiers(
    val avatar: Modifier,
    val name: Modifier,
) {
    companion object {
        val None = GroupSharedModifiers(
            avatar = Modifier,
            name = Modifier,
        )
    }
}

/*
 * Why: the group's photo (or initials) and name move from the row tapped into the group's header and back (beta
 * feedback of 2026-11-12). The avatar keeps its square as it grows, the name scales from the row's size to the bar's.
 * Without a group or a scope (beside the list, previews) nothing is shared. A system with animations off ends it at
 * once, as Compose scales every animation by the system's animator duration.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun rememberGroupSharedModifiers(groupId: String?): GroupSharedModifiers {
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (groupId == null || transitionScope == null || visibilityScope == null) return GroupSharedModifiers.None
    return with(transitionScope) {
        GroupSharedModifiers(
            avatar = Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(GroupSharedKey(groupId, GroupSharedElement.AVATAR)),
                animatedVisibilityScope = visibilityScope,
            ),
            name = Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(GroupSharedKey(groupId, GroupSharedElement.NAME)),
                animatedVisibilityScope = visibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                ),
            ),
        )
    }
}

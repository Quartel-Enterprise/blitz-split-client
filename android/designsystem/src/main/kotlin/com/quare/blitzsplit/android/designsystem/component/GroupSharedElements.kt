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

// Why: where the group's or the bill's rows and screen fly from and to; null where nothing flies (beside the list).
val LocalSharedOrigin = compositionLocalOf<SharedOrigin?> { null }

// Why: the modifiers a group's avatar, name and balance take to fly between its row or card and its screen's header.
@Immutable
class GroupSharedModifiers(
    val avatar: Modifier,
    val name: Modifier,
    // Why: "+R$ 313,81", which lands on "Your balance in this group"'s amount (beta feedback of 2026-11-15).
    val balance: Modifier,
    // Why: "to receive" or "you owe" under or beside it.
    val balanceWord: Modifier,
) {
    companion object {
        val None = GroupSharedModifiers(
            avatar = Modifier,
            name = Modifier,
            balance = Modifier,
            balanceWord = Modifier,
        )
    }
}

/*
 * Why: the group's photo (or initials), name and balance move from the row tapped into the group's header and back
 * (beta feedback of 2026-11-12 and 2026-11-15). The avatar keeps its square as it grows, the texts scale from the row's
 * size to the screen's. Keyed by the group and the screen it opened from, so only the same group's pieces fly. Without
 * a group or a scope (beside the list, previews) nothing is shared. A system with animations off ends it at once, as
 * Compose scales every animation by the system's animator duration.
 */
@Composable
fun rememberGroupSharedModifiers(groupId: String?): GroupSharedModifiers {
    val scopes = rememberSharedScopes(groupId) ?: return GroupSharedModifiers.None
    return GroupSharedModifiers(
        avatar = scopes.element("group-avatar-${scopes.key}"),
        name = scopes.text("group-name-${scopes.key}"),
        balance = scopes.text("group-balance-${scopes.key}"),
        balanceWord = scopes.text("group-balance-word-${scopes.key}"),
    )
}

// Why: what every shared element of a screen needs, read once: the two scopes and the key of the row it flies with.
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedScopes internal constructor(
    private val transitionScope: SharedTransitionScope,
    private val visibilityScope: AnimatedVisibilityScope,
    val key: String,
) {
    // Why: a shape (an avatar, a category's badge) that keeps its own look as its bounds move and resize.
    @Composable
    fun element(id: String): Modifier = with(transitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(id),
            animatedVisibilityScope = visibilityScope,
        )
    }

    // Why: a text, whose two sides crossfade while it scales from one size to the other, from its start.
    @Composable
    fun text(id: String): Modifier = with(transitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(id),
            animatedVisibilityScope = visibilityScope,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            ),
        )
    }
}

// Why: null where nothing flies: no id, no navigation's scope (previews, tests) or no origin (beside the list).
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberSharedScopes(id: String?): SharedScopes? {
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    val origin = LocalSharedOrigin.current
    if (id == null || transitionScope == null || visibilityScope == null || origin == null) return null
    return SharedScopes(
        transitionScope = transitionScope,
        visibilityScope = visibilityScope,
        key = "$origin-$id",
    )
}

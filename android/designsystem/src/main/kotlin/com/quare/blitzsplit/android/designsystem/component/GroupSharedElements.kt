package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout

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

// Why: the modifiers a group's avatar, name, balance and faces take to fly between its row or card and its screen's header.
@Immutable
class GroupSharedModifiers(
    val avatar: Modifier,
    val name: Modifier,
    // Why: "+R$ 313,81", which lands on "Your balance in this group"'s amount (beta feedback of 2026-11-15).
    val balance: Modifier,
    // Why: "to receive" or "you owe" under or beside it.
    val balanceWord: Modifier,
    // Why: the members' stacked faces, which land on the group's faces under its header (beta feedback of 2026-11-16).
    val faces: Modifier,
) {
    companion object {
        val None = GroupSharedModifiers(
            avatar = Modifier,
            name = Modifier,
            balance = Modifier,
            balanceWord = Modifier,
            faces = Modifier,
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
        name = scopes.scaled("group-name-${scopes.key}"),
        balance = scopes.scaled("group-balance-${scopes.key}"),
        balanceWord = scopes.scaled("group-balance-word-${scopes.key}"),
        // Why: scaled, so the row's small stack grows into the header's, whatever faces each one shows.
        faces = scopes.scaled("group-faces-${scopes.key}"),
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

    /*
     * Why: a text or a stack of faces, whose two sides crossfade while it scales from one size to the other, from its
     * start: a name, an amount, the faces of a group or of who paid. Placed only for real (skipsBaselinePlacement), so
     * one in a top bar flies in as it flies back.
     */
    @Composable
    fun scaled(id: String): Modifier = with(transitionScope) {
        Modifier.skipsBaselinePlacement().sharedBounds(
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
internal fun rememberSharedScopes(
    id: String?,
    origin: SharedOrigin? = LocalSharedOrigin.current,
): SharedScopes? {
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (id == null || transitionScope == null || visibilityScope == null || origin == null) return null
    return remember(transitionScope, visibilityScope, origin, id) {
        SharedScopes(
            transitionScope = transitionScope,
            visibilityScope = visibilityScope,
            key = sharedKeyOf(origin, id),
        )
    }
}

// Why: the one key format both sides of a flight build, so a screen keying by another id still meets its row.
internal fun sharedKeyOf(
    origin: SharedOrigin,
    id: String,
): String = "$origin-$id"

/*
 * Why: Material's top bars read their title's baseline while they measure, which places the title once ahead of the
 * real placement, only to compute the line. A shared text placed there took the screen's own spot as where it flies
 * from, so the group's name popped into the header instead of flying in, while it flew back fine (beta feedback of
 * 2026-11-16). Placed only for real, the flight starts from the row's text. Every scaled shared element takes it, in a top bar or not: none sits in a parent that
 * aligns by its baseline, and the alignment-only placement is never where a flight should start.
 */
private fun Modifier.skipsBaselinePlacement(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        // Why: no coordinates while the parent places its children only to read their alignment lines.
        if (coordinates != null) placeable.place(0, 0)
    }
}

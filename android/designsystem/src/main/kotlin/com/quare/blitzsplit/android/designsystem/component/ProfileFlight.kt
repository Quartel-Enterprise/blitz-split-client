package com.quare.blitzsplit.android.designsystem.component

import android.os.SystemClock
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import java.util.UUID

/*
 * Why: the face tapped last and the faces whose profiles are open, so a profile's photo flies out of the very face
 * tapped and back into it (beta feedback of 2026-11-15), never out of another face of the same person on that screen
 * (a bill shows its payer twice) nor out of a face nobody tapped (a profile opened from a search hit or a link).
 */
@Stable
class ProfileFlightState {
    private var lastTap by mutableStateOf<Pair<String, Long>?>(null)
    private val openFaces = mutableStateSetOf<String>()

    internal fun onTapped(faceId: String) {
        lastTap = faceId to SystemClock.uptimeMillis()
    }

    // Why: the face a profile opening now flies out of: the one tapped in the last second, else none.
    internal fun recentFaceOrNull(): String? = lastTap
        ?.takeIf { (_, at) -> SystemClock.uptimeMillis() - at <= TAP_WINDOW_MILLIS }
        ?.first

    internal fun isFlying(faceId: String): Boolean = lastTap?.first == faceId || faceId in openFaces

    internal fun onProfileShown(faceId: String) {
        openFaces += faceId
    }

    internal fun onProfileGone(faceId: String) {
        openFaces -= faceId
    }

    private companion object {
        // Why: how recent a tap on a face must be for the profile opening now to fly out of it, as the group's avatar waits.
        const val TAP_WINDOW_MILLIS = 1_000L
    }
}

// Why: null in previews and tests, and wherever no navigation draws the screens, where no face flies.
val LocalProfileFlight = staticCompositionLocalOf<ProfileFlightState?> { null }

// Why: the face's own modifier, and the tap to send along with the profile's opening.
@Stable
class ProfileFlightSource internal constructor(
    val face: Modifier,
    private val onTap: () -> Unit,
) {
    // Why: wraps the tap that opens the profile, so the face is the one marked before the profile opens.
    fun toFlyingClick(onClick: () -> Unit): () -> Unit = {
        onTap()
        onClick()
    }

    // Why: as toFlyingClick, for a face that opens nothing (someone no longer in the group), which stays null.
    fun toFlyingClickOrNull(onClick: (() -> Unit)?): (() -> Unit)? = onClick?.let(::toFlyingClick)

    companion object {
        val None = ProfileFlightSource(
            face = Modifier,
            onTap = {},
        )
    }
}

/*
 * Why: a face that opens a profile (a top bar's avatar, a split's row, a member, an actor in the feed). It carries the
 * shared element only while it is the face tapped last or the face of a profile still open, so a list of faces costs
 * nothing until one is tapped. Where nothing flies (a dialog's bill, previews) it is None.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun rememberProfileFlightSource(): ProfileFlightSource {
    val faceId = rememberSaveable { UUID.randomUUID().toString() }
    val flight = LocalProfileFlight.current
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (flight == null || transitionScope == null || visibilityScope == null) return ProfileFlightSource.None
    // Why: only the faces whose answer changes redraw on a tap, not every face of the list.
    val isFlying by remember(flight, faceId) { derivedStateOf { flight.isFlying(faceId) } }
    val face = if (isFlying) {
        with(transitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(faceKeyOf(faceId)),
                animatedVisibilityScope = visibilityScope,
            )
        }
    } else {
        Modifier
    }
    return ProfileFlightSource(
        face = face,
        onTap = { flight.onTapped(faceId) },
    )
}

/*
 * Why: the photo at the top of Profile or a contact's profile, which the face tapped flies into and back out of. The
 * face is read once, as the profile first draws, and kept with it, so back still finds it; a profile opened without a
 * tap on a face (a search hit, a link, beside Contacts' list) flies nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun rememberProfileFlightTarget(): Modifier {
    val flight = LocalProfileFlight.current
    val faceId = rememberSaveable { flight?.recentFaceOrNull().orEmpty() }.ifEmpty { null }
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (flight == null || faceId == null || transitionScope == null || visibilityScope == null) return Modifier
    DisposableEffect(flight, faceId) {
        flight.onProfileShown(faceId)
        onDispose { flight.onProfileGone(faceId) }
    }
    return with(transitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(faceKeyOf(faceId)),
            animatedVisibilityScope = visibilityScope,
        )
    }
}

private fun faceKeyOf(faceId: String): String = "profile-face-$faceId"

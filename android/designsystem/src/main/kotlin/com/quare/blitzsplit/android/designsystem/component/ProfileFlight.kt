package com.quare.blitzsplit.android.designsystem.component

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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

    /*
     * Why: the face a profile opening now flies out of: the one tapped in the last second, else none. The tap is used
     * up, so it counts only for the profile it opened, never for one opened another way right after; the face stays
     * flying as that profile's.
     */
    internal fun takeRecentFaceOrNull(): String? {
        val face = lastTap
            ?.takeIf { (_, at) -> SystemClock.uptimeMillis() - at <= TAP_WINDOW_MILLIS }
            ?.first
        lastTap = null
        face?.let { openFaces += it }
        return face
    }

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

/*
 * Why: the face's own modifier, the name's and the balance's (its amount and its word) for a row that shows them, and
 * the tap to send along with the profile's opening.
 */
@Stable
class ProfileFlightSource internal constructor(
    val face: Modifier,
    val name: Modifier,
    val amount: Modifier,
    val word: Modifier,
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
            name = Modifier,
            amount = Modifier,
            word = Modifier,
            onTap = {},
        )
    }
}

/*
 * Why: the pieces that fly, alike in the row tapped and in the profile they land in: the face, and, where both show them,
 * the name and the balance's amount and word. A piece one side leaves out (Profile has no balance) finds no twin there
 * and stays in place.
 */
@Stable
class ProfileFlightPieces internal constructor(
    val face: Modifier,
    val name: Modifier,
    val amount: Modifier,
    val word: Modifier,
) {
    companion object {
        val None = ProfileFlightPieces(
            face = Modifier,
            name = Modifier,
            amount = Modifier,
            word = Modifier,
        )
    }
}

/*
 * Why: a face that opens a profile (a top bar's avatar, a split's row, a member, an actor in the feed). It carries the
 * shared element only while it is the face tapped last or the face of a profile still open, so a list of faces costs
 * nothing until one is tapped. Where nothing flies (a dialog's bill, previews) it is None. A row that also shows the
 * person's name and balance (Contacts, Balances) puts name, amount and word on them, which fly into the profile's own
 * (beta feedback of 2026-11-16). `balanceKey` is the currency of the row's balance: Balances lists a person once per
 * currency, and only the row in the currency the profile shows has a balance to fly into.
 */
@Composable
fun rememberProfileFlightSource(balanceKey: String? = null): ProfileFlightSource {
    val faceId = rememberSaveable { UUID.randomUUID().toString() }
    val flight = LocalProfileFlight.current
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (flight == null || transitionScope == null || visibilityScope == null) return ProfileFlightSource.None
    // Why: only the faces whose answer changes redraw on a tap, not every face of the list.
    val isFlying by remember(flight, faceId) { derivedStateOf { flight.isFlying(faceId) } }
    val pieces = if (isFlying) {
        rememberFlyingPieces(
            faceId = faceId,
            balanceKey = balanceKey,
            transitionScope = transitionScope,
            visibilityScope = visibilityScope,
        )
    } else {
        ProfileFlightPieces.None
    }
    return ProfileFlightSource(
        face = pieces.face,
        name = pieces.name,
        amount = pieces.amount,
        word = pieces.word,
        onTap = { flight.onTapped(faceId) },
    )
}

/*
 * Why: the photo at the top of Profile or a contact's profile, which the face tapped flies into and back out of, with
 * the name and the balance where the profile shows them. The face is read once, as the profile first draws, and kept
 * with it, so back still finds it; a profile opened without a tap on a face (a search hit, a link, beside Contacts'
 * list) flies nothing. `balanceKey` is the currency of the balance the profile shows, as for the row.
 */
@Composable
fun rememberProfileFlightTarget(balanceKey: String? = null): ProfileFlightPieces {
    val flight = LocalProfileFlight.current
    val faceId = rememberSaveable { flight?.takeRecentFaceOrNull().orEmpty() }.ifEmpty { null }
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalEntryVisibilityScope.current
    if (flight == null || faceId == null || transitionScope == null || visibilityScope == null) {
        return ProfileFlightPieces.None
    }
    DisposableEffect(flight, faceId) {
        flight.onProfileShown(faceId)
        onDispose { flight.onProfileGone(faceId) }
    }
    return rememberFlyingPieces(
        faceId = faceId,
        balanceKey = balanceKey,
        transitionScope = transitionScope,
        visibilityScope = visibilityScope,
    )
}

/*
 * Why: the face keeps its shape and flies as one element. The texts change size and weight, and the word its wording
 * ("owes you" in the row, "Larissa owes you" in the profile), so they fly as shared bounds, their content scaled to the
 * bounds and cross-faded.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberFlyingPieces(
    faceId: String,
    balanceKey: String?,
    transitionScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
): ProfileFlightPieces = with(transitionScope) {
    ProfileFlightPieces(
        face = Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(pieceKeyOf("face", faceId)),
            animatedVisibilityScope = visibilityScope,
        ),
        name = Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(pieceKeyOf("name", faceId)),
            animatedVisibilityScope = visibilityScope,
        ),
        amount = Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(pieceKeyOf("amount-$balanceKey", faceId)),
            animatedVisibilityScope = visibilityScope,
        ),
        word = Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(pieceKeyOf("word-$balanceKey", faceId)),
            animatedVisibilityScope = visibilityScope,
        ),
    )
}

private fun pieceKeyOf(
    piece: String,
    faceId: String,
): String = "profile-$piece-$faceId"

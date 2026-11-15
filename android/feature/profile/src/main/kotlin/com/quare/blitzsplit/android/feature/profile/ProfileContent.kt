package com.quare.blitzsplit.android.feature.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.quare.blitzsplit.android.designsystem.component.rememberProfileFlightTarget
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

/*
 * Why: the face tapped (a top bar's avatar, "You" in a bill) flies into the identity card's avatar and back (beta
 * feedback of 2026-11-15).
 */
internal val LocalProfileFaceFlight = staticCompositionLocalOf<Modifier> { Modifier }

@Composable
fun ProfileContent(
    state: ProfileUiState,
    appVersion: String,
    onEvent: (ProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Why: read as the profile first draws, before Room is, so the face tapped is still the last second's.
    CompositionLocalProvider(LocalProfileFaceFlight provides rememberProfileFlightTarget()) {
        ProfileScaffold(
            state = state,
            appVersion = appVersion,
            onEvent = onEvent,
            modifier = modifier,
            snackbarHostState = snackbarHostState,
        )
    }
}

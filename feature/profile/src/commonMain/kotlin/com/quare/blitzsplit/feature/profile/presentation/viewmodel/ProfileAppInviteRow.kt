package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInvite
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInviteStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Why: Profile's "App on your phone" (14.4): "See code" opens the app's code, its ✕ ends the invite everywhere.
internal class ProfileAppInviteRow(
    private val uiState: StateFlow<ProfileUiState>,
    private val navigator: Navigator,
    private val dismissAppInvite: DismissAppInvite,
    private val trackEvent: TrackEvent,
    private val scope: CoroutineScope,
) {
    private var isDismissing = false

    // Why: never twice, so a double click doesn't stack the code over itself.
    fun openCode() {
        if (uiState.value.appInvite == null) return
        if (navigator.backStack.value.lastOrNull() is Route.AppDownload) return
        navigator.navigate(Route.AppDownload(AppDownloadSource.PROFILE))
    }

    // Why: once, also for a second click before Room says the invite went.
    fun dismiss() {
        if (uiState.value.appInvite?.status != ProfileAppInviteStatus.ACTIVE || isDismissing) return
        isDismissing = true
        trackEvent("app_invite_dismissed", mapOf("source" to "profile"))
        scope.launch { dismissAppInvite() }
    }

    // Why: the guard holds until Room says the row is no longer highlighted, not only until the write ends.
    fun onInviteChanged(invite: ProfileAppInvite?) {
        if (invite?.status != ProfileAppInviteStatus.ACTIVE) isDismissing = false
    }
}

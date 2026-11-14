package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.LinkLanguage
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * Why: Profile's "Share profile" and "Copy link", the account's profile link (blitzsplit.com/u/<code>). Each acts only
 * while the account has a link, so a tap from a stale screen never shares nothing.
 */
internal class ProfileShareLink(
    private val uiState: StateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val observeSpokenLanguage: ObserveSpokenLanguage,
    private val trackEvent: TrackEvent,
    private val emit: (ProfileUiAction) -> Unit,
) {
    fun share() {
        val link = uiState.value.shareLink ?: return
        trackEvent("profile_share_clicked", emptyMap())
        scope.launch { emit(ProfileUiAction.ShareProfileLink(toSpokenLanguage(link))) }
    }

    fun copy() {
        val link = uiState.value.shareLink ?: return
        trackEvent("profile_link_copied", emptyMap())
        scope.launch { emit(ProfileUiAction.CopyProfileLink(toSpokenLanguage(link))) }
    }

    // Why: a profile has nothing else to pick its link's preview language by, so the link carries the app's own.
    private suspend fun toSpokenLanguage(link: String): String =
        LinkLanguage.buildLink(link, observeSpokenLanguage().first())
}

package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.flow.StateFlow

/*
 * Why: Profile's "Share profile" and "Copy link", the account's profile link (blitzsplit.com/u/<code>). Each acts only
 * while the account has a link, so a tap from a stale screen never shares nothing.
 */
internal class ProfileShareLink(
    private val uiState: StateFlow<ProfileUiState>,
    private val trackEvent: TrackEvent,
    private val emit: (ProfileUiAction) -> Unit,
) {
    fun share() {
        val link = uiState.value.shareLink ?: return
        trackEvent("profile_share_clicked", emptyMap())
        emit(ProfileUiAction.ShareProfileLink(link))
    }

    fun copy() {
        val link = uiState.value.shareLink ?: return
        trackEvent("profile_link_copied", emptyMap())
        emit(ProfileUiAction.CopyProfileLink(link))
    }
}

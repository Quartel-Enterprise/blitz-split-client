package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/*
 * Why: Profile's dialogs and sheets (who sees the profile, Appearance, Disconnect, Sign out or Erase profile and sign
 * out). A sign out or unlink under way keeps its dialog, which shows how it ends, so none opens or closes then; each
 * tap that opens one counts only once it opened.
 */
internal class ProfileDialogs(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val trackEvent: TrackEvent,
) {
    // Why: the dialogs that ask before signing out, erasing or disconnecting, whose close is an answer.
    private val confirmations = setOf(
        ProfileDialog.SIGN_OUT,
        ProfileDialog.SIGN_OUT_PENDING,
        ProfileDialog.ERASE_PROFILE,
        ProfileDialog.UNLINK_ACCOUNT,
    )

    fun openVisibilityInfo() {
        if (show(ProfileDialog.VISIBILITY_INFO)) trackEvent("profile_visibility_info_clicked", emptyMap())
    }

    // Why: the theme picked shows only once it was read, never the default guessed.
    fun openAppearance() {
        if (uiState.value.isLoaded && show(ProfileDialog.APPEARANCE)) {
            trackEvent("profile_appearance_clicked", emptyMap())
        }
    }

    // Why: only where the switch it explains shows, and over Appearance, where its button is.
    fun openDynamicColorsInfo() {
        val state = uiState.value
        if (state.showsDynamicColors && state.dialog == ProfileDialog.APPEARANCE &&
            show(ProfileDialog.DYNAMIC_COLORS_INFO)
        ) {
            trackEvent("profile_dynamic_colors_info_clicked", emptyMap())
        }
    }

    // Why: a sign out on its way would end the account the unlink keeps, so the row waits for it.
    fun openUnlink() {
        val state = uiState.value
        if (state.hasAccount && !state.isSignOutScheduled && show(ProfileDialog.UNLINK_ACCOUNT)) {
            trackEvent("profile_account_clicked", emptyMap())
        }
    }

    fun openSignOut() {
        val state = uiState.value
        if (!state.canSignOut) return
        val dialog = signOutDialogOf(state)
        if (show(dialog)) trackEvent("sign_out_clicked", mapOf("dialog" to dialog))
    }

    /*
     * Why: the info over Appearance goes back to it; any other closes to the profile. Only a confirmation's close
     * counts as cancelled: the info dialogs close with their only button and Appearance has none.
     */
    fun close() {
        if (uiState.value.isSigningOut || uiState.value.isUnlinking) return
        uiState.value.dialog?.takeIf { it in confirmations }?.let {
            trackEvent("profile_dialog_cancelled", mapOf("dialog" to it))
        }
        uiState.update {
            it.copy(
                dialog = if (it.dialog == ProfileDialog.DYNAMIC_COLORS_INFO) ProfileDialog.APPEARANCE else null,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
                hasUnlinkFailed = false,
            )
        }
    }

    // Why: says whether it opened, which Analytics counts.
    private fun show(dialog: ProfileDialog): Boolean {
        if (uiState.value.isSigningOut || uiState.value.isUnlinking) return false
        uiState.update {
            it.copy(
                dialog = dialog,
                hasConfirmedCodeSaved = false,
                hasSignOutFailed = false,
                hasUnlinkFailed = false,
            )
        }
        return true
    }

    /*
     * Why: without Google or Apple the profile only comes back with the recovery code, so erasing asks for it to be
     * kept. With an account, changes that haven't gone up would be lost, so the sign out waits for them.
     */
    private fun signOutDialogOf(state: ProfileUiState): ProfileDialog = when {
        !state.hasAccount -> ProfileDialog.ERASE_PROFILE
        state.pendingChangeCount > 0 -> ProfileDialog.SIGN_OUT_PENDING
        else -> ProfileDialog.SIGN_OUT
    }
}

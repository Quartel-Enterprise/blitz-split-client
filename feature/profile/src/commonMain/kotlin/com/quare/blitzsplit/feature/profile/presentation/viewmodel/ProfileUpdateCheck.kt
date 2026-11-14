package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import com.quare.blitzsplit.feature.profile.presentation.model.UpdateCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: Perfil's "Buscar atualizações", as Bible Planner's: an update found opens what opening the app would (Google
 * Play's flow, or "Atualização disponível"), even within the hour; nothing to offer says so.
 */
internal class ProfileUpdateCheck(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val checkForUpdate: CheckForUpdate,
    private val showUpdatePrompt: ShowUpdatePrompt,
    private val trackEvent: TrackEvent,
    private val emit: (ProfileUiAction) -> Unit,
) {
    fun check() {
        if (!uiState.value.showsCheckForUpdates || uiState.value.isCheckingForUpdates) return
        trackEvent("check_for_updates_clicked", emptyMap())
        uiState.update { it.copy(isCheckingForUpdates = true) }
        scope.launch {
            val availability = checkForUpdate()
            uiState.update { it.copy(isCheckingForUpdates = false) }
            val result = when (availability) {
                is UpdateAvailability.Pending -> return@launch showUpdatePrompt(availability, UpdatePromptSource.MANUAL)
                UpdateAvailability.Downloading -> UpdateCheckResult.DOWNLOADING
                UpdateAvailability.NotAvailable -> UpdateCheckResult.UP_TO_DATE
                UpdateAvailability.CheckFailed -> UpdateCheckResult.FAILED
            }
            emit(ProfileUiAction.ShowUpdateCheckResult(result))
        }
    }
}

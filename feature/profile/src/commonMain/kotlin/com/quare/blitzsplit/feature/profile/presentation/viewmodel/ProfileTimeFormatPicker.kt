package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateTimeFormatPreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: Preferences' "Time format", this device's pick (System, 24-hour or 12-hour), which every time of day the app
 * draws follows through the app's root (AppViewModel.timeFormat). The stored one is read with the rest of the profile,
 * so isLoaded waits for it (ProfileViewModel).
 */
internal class ProfileTimeFormatPicker(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val updateTimeFormat: UpdateTimeFormatPreference,
    private val trackEvent: TrackEvent,
    private val scope: CoroutineScope,
) {
    // Why: the last pick not stored yet, so an older stored value still on its way never shows over it.
    private var pending: TimeFormatPreference? = null

    // Why: Android's dialog closes on the pick; only another format counts and is written.
    fun select(timeFormat: TimeFormatPreference) {
        if (uiState.value.dialog == ProfileDialog.TIME_FORMAT) uiState.update { it.copy(dialog = null) }
        if (timeFormat == uiState.value.timeFormat) return
        trackEvent("time_format_selected", mapOf("time_format" to timeFormat))
        pending = timeFormat
        uiState.update { it.copy(timeFormat = timeFormat) }
        scope.launch { updateTimeFormat(timeFormat) }
    }

    // Why: what the state shows for a stored format: the pending pick until the store reaches it, then the store's.
    fun toShown(stored: TimeFormatPreference): TimeFormatPreference {
        val pick = pending ?: return stored
        if (pick == stored) pending = null
        return pick
    }
}

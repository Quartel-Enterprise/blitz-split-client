package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveTimeFormatPreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateTimeFormatPreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: Preferences' "Time format", this device's pick (System, 24-hour or 12-hour), which every time of day the app
 * draws follows through the app's root (AppViewModel.timeFormat).
 */
internal class ProfileTimeFormatPicker(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val updateTimeFormat: UpdateTimeFormatPreference,
    private val trackEvent: TrackEvent,
    private val scope: CoroutineScope,
) {
    fun follow(observeTimeFormat: ObserveTimeFormatPreference) {
        observeTimeFormat()
            .onEach { timeFormat -> uiState.update { it.copy(timeFormat = timeFormat) } }
            .launchIn(scope)
    }

    // Why: Android's dialog closes on the pick; only another format counts and is written.
    fun select(timeFormat: TimeFormatPreference) {
        if (uiState.value.dialog == ProfileDialog.TIME_FORMAT) uiState.update { it.copy(dialog = null) }
        if (timeFormat == uiState.value.timeFormat) return
        trackEvent("time_format_selected", mapOf("time_format" to timeFormat))
        uiState.update { it.copy(timeFormat = timeFormat) }
        scope.launch { updateTimeFormat(timeFormat) }
    }
}

package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateAppLanguage
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileDialog
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiAction
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

// Why: Preferences' "Language", this device's pick, which each platform applies its own way.
internal class ProfileLanguagePicker(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val updateAppLanguage: UpdateAppLanguage,
    private val trackEvent: TrackEvent,
    private val emit: (ProfileUiAction) -> Unit,
) {
    fun follow(
        observeAppLanguage: ObserveAppLanguage,
        scope: CoroutineScope,
    ) {
        observeAppLanguage()
            .onEach { language -> uiState.update { it.copy(language = language) } }
            .launchIn(scope)
    }

    /*
     * Why: only another language counts and applies; Android's sheet closes on the pick, as Bible Planner's does,
     * and the platform redraws in it.
     */
    fun select(language: AppLanguage) {
        if (uiState.value.dialog ==
            ProfileDialog.LANGUAGE
        ) {
            uiState.update {
                it.copy(
                    dialog = null,
                    languageQuery = "",
                )
            }
        }
        if (language == uiState.value.language) return
        trackEvent("language_selected", mapOf("language" to language))
        updateAppLanguage(language)
        emit(ProfileUiAction.ApplyLanguage)
    }

    // Why: the search only exists while the picker is open, so a query typed after it closed is dropped.
    fun search(query: String) {
        if (uiState.value.dialog != ProfileDialog.LANGUAGE) return
        uiState.update { it.copy(languageQuery = query) }
    }
}

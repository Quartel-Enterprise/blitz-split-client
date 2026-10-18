package com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameRules
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileSetupViewModel(
    private val observeProfile: ObserveProfile,
    private val getSessionUser: GetSessionUser,
    private val validateDisplayName: ValidateDisplayName,
    private val saveProfile: SaveProfile,
    private val navigator: Navigator,
) : ViewModel() {
    val uiState: StateFlow<ProfileSetupUiState>
        field = MutableStateFlow(
            ProfileSetupUiState(
                name = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                showsNameTooShort = false,
                canContinue = false,
                isSaving = false,
            ),
        )

    val stateWatcher: FlowWatcher<ProfileSetupUiState> = FlowWatcher(uiState)

    init {
        prefill()
    }

    fun onEvent(event: ProfileSetupUiEvent) {
        when (event) {
            is ProfileSetupUiEvent.OnNameChanged -> updateName(event.name)
            is ProfileSetupUiEvent.OnAvatarColorSelected -> uiState.update { it.copy(avatarColor = event.color) }
            ProfileSetupUiEvent.OnContinueClicked -> save()
            ProfileSetupUiEvent.OnBackClicked -> navigator.navigateBack()
        }
    }

    // Why: Google and Apple already know the name and photo, and a profile edited before stays as is.
    private fun prefill() {
        viewModelScope.launch {
            val existing = observeProfile().first()
            val sessionUser = getSessionUser()
            val name = existing?.displayName ?: sessionUser?.displayName.orEmpty()
            uiState.update { state ->
                if (state.name.isNotEmpty()) return@update state
                state.copy(
                    name = name.take(DisplayNameRules.MAX_LENGTH),
                    avatarColor = existing?.avatarColor ?: state.avatarColor,
                    photoUrl = existing?.photoUrl ?: sessionUser?.photoUrl,
                    canContinue = validateDisplayName(name) == DisplayNameValidation.VALID,
                )
            }
        }
    }

    private fun updateName(name: String) {
        val limited = name.take(DisplayNameRules.MAX_LENGTH)
        val validation = validateDisplayName(limited)
        uiState.update { state ->
            state.copy(
                name = limited,
                showsNameTooShort = limited.isNotBlank() && validation == DisplayNameValidation.TOO_SHORT,
                canContinue = validation == DisplayNameValidation.VALID,
            )
        }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canContinue || state.isSaving) return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveProfile(
                UserProfile(displayName = state.name, avatarColor = state.avatarColor, photoUrl = state.photoUrl),
            )
            navigator.replaceAll(Route.Groups)
        }
    }
}

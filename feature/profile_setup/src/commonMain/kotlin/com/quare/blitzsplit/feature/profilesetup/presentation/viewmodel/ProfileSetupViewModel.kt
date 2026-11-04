package com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.model.NameError
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
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<ProfileSetupUiEvent>(trackEvent) {
    val uiState: StateFlow<ProfileSetupUiState>
        field = MutableStateFlow(
            ProfileSetupUiState(
                name = "",
                avatarColor = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                nameError = null,
                canContinue = false,
                isSaving = false,
            ),
        )

    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = { editor -> uiState.update { it.copy(photoCrop = editor) } },
        onPrepared = ::showPhoto,
        // Why: once Continue is waiting on a pick, a later one would cancel the photo being saved.
        isBusy = { uiState.value.isSaving },
    )

    val stateWatcher: FlowWatcher<ProfileSetupUiState> = FlowWatcher(uiState)

    init {
        prefill()
    }

    override fun handleEvent(event: ProfileSetupUiEvent) {
        when (event) {
            is ProfileSetupUiEvent.OnNameChanged -> updateName(event.name)
            is ProfileSetupUiEvent.OnAvatarColorSelected -> uiState.update { it.copy(avatarColor = event.color) }
            is ProfileSetupUiEvent.OnPhotoPicked -> photoPicker.prepare(event.source)
            is ProfileSetupUiEvent.OnPhotoCrop -> photoPicker.onEvent(event.event)
            ProfileSetupUiEvent.OnPhotoUnreadable -> photoPicker.fail()
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
            val validation = validateDisplayName(name)
            uiState.update { state ->
                if (state.name.isNotEmpty()) return@update state
                state.copy(
                    name = name,
                    avatarColor = existing?.avatarColor ?: state.avatarColor,
                    photo = state.photo ?: existing?.photo ?: sessionUser?.photoUrl?.let(ProfilePhoto::Remote),
                    nameError = nameErrorOf(name, validation),
                    canContinue = validation == DisplayNameValidation.VALID,
                )
            }
        }
    }

    // Why: a name over the limit stays as typed with an error, so a pasted or prefilled name is never cut unseen.
    private fun updateName(name: String) {
        val validation = validateDisplayName(name)
        uiState.update { state ->
            state.copy(
                name = name,
                nameError = nameErrorOf(name, validation),
                canContinue = validation == DisplayNameValidation.VALID,
            )
        }
    }

    private fun nameErrorOf(
        name: String,
        validation: DisplayNameValidation,
    ): NameError? = when {
        validation == DisplayNameValidation.TOO_LONG -> NameError.TOO_LONG
        validation == DisplayNameValidation.TOO_SHORT && name.isNotBlank() -> NameError.TOO_SHORT
        else -> null
    }

    private fun showPhotoStatus(
        isPreparing: Boolean,
        hasFailed: Boolean,
    ) {
        uiState.update {
            it.copy(
                isPreparingPhoto = isPreparing,
                showsPhotoError = hasFailed,
            )
        }
    }

    private fun showPhoto(photo: ProfilePhoto) {
        uiState.update {
            it.copy(
                photo = photo,
                isPreparingPhoto = false,
            )
        }
    }

    /*
     * Why: Continue right after "Pronto" waits for the photo instead of saving the profile without it. If that
     * fails, or the pick it waited on opened "Ajustar foto", the screen stays so the error or the editor is seen; a
     * later Continue saves without it.
     */
    private fun save() {
        if (!uiState.value.canContinue || uiState.value.isSaving || uiState.value.photoCrop != null) return
        uiState.update { it.copy(isSaving = true) }
        val pickInProgress = photoPicker.pickInProgress
        viewModelScope.launch {
            pickInProgress?.join()
            if (pickInProgress != null && (uiState.value.showsPhotoError || uiState.value.photoCrop != null)) {
                uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val state = uiState.value
            saveProfile(
                UserProfile(
                    displayName = state.name,
                    avatarColor = state.avatarColor,
                    photo = state.photo,
                ),
            )
            navigator.replaceAll(Route.Home)
        }
    }
}

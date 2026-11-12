package com.quare.blitzsplit.feature.editprofile.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountPhoto
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.guardUnsavedInput
import com.quare.blitzsplit.core.navigation.navigateBackFrom
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
import com.quare.blitzsplit.feature.editprofile.presentation.model.EDIT_PROFILE_SOURCE
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileNameError
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val validateDisplayName: ValidateDisplayName,
    private val saveProfile: SaveProfile,
    private val navigator: Navigator,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    observeProfile: ObserveProfile,
    observeAccountPhoto: ObserveAccountPhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<EditProfileUiEvent>(trackEvent) {
    val uiState: StateFlow<EditProfileUiState>
        field = MutableStateFlow(
            EditProfileUiState(
                isLoaded = false,
                name = "",
                avatarColor = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                nameError = null,
                isNameValid = false,
                hasChanges = false,
                isSaving = false,
                accountProvider = null,
                accountPhotoUrl = null,
            ),
        )

    // Why: what the form compares against, kept up to date as sync replaces a pending photo with its upload.
    private var saved: UserProfile? = null

    private var isAccountRead = false

    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        showEditor = ::showPhotoCrop,
        onPrepared = ::showPhoto,
        // Why: Save sends the photo shown.
        isBusy = { uiState.value.isSaving },
    )

    val stateWatcher: FlowWatcher<EditProfileUiState> = FlowWatcher(uiState)

    init {
        navigator.guardUnsavedInput(viewModelScope, Route.EditProfile, uiState.map { it.hasUnsavedInput })
        observeProfile()
            .filterNotNull()
            .onEach(::onSavedProfileChanged)
            .launchIn(viewModelScope)
        observeAccountPhoto()
            .onEach { account ->
                // Why: the notice names the provider at once; the picture follows once the session is read.
                isAccountRead = true
                uiState.update {
                    it.copy(
                        isLoaded = saved != null,
                        accountProvider = account.provider,
                        accountPhotoUrl = account.photoUrl,
                    )
                }
            }.launchIn(viewModelScope)
    }

    // Why: the form takes edits only once it holds the saved profile, which would otherwise replace them.
    override fun isAccepted(event: EditProfileUiEvent): Boolean = when {
        event == EditProfileUiEvent.OnBackClicked -> true

        !uiState.value.isLoaded -> false

        // Why: the picker drops a pick and its Done while Save sends the photo shown, so neither counts then.
        uiState.value.isSaving -> !event.isDroppedWhileSaving()

        else -> true
    }

    private fun EditProfileUiEvent.isDroppedWhileSaving(): Boolean = this is EditProfileUiEvent.OnPhotoPicked ||
        this == EditProfileUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked)

    override fun handleEvent(event: EditProfileUiEvent) {
        when (event) {
            is EditProfileUiEvent.OnNameChanged -> edit { it.withName(event.name) }

            is EditProfileUiEvent.OnAvatarColorSelected -> edit { it.copy(avatarColor = event.color) }

            is EditProfileUiEvent.OnPhotoPicked -> photoPicker.prepare(event.source)

            is EditProfileUiEvent.OnPhotoCrop -> photoPicker.onEvent(event.event)

            EditProfileUiEvent.OnPhotoUnreadable -> photoPicker.fail()

            EditProfileUiEvent.OnAccountPhotoSelected -> useAccountPhoto()

            EditProfileUiEvent.OnPhotoRemoved -> removePhoto()

            EditProfileUiEvent.OnAvatarClicked -> navigator.navigate(Route.ProfilePhoto)

            EditProfileUiEvent.OnSaveClicked -> {
                trackEvent("edit_profile_save_clicked", emptyMap())
                save()
            }

            EditProfileUiEvent.OnDiscardClicked -> discard()

            EditProfileUiEvent.OnBackClicked -> navigator.navigateBackFrom(Route.EditProfile)
        }
    }

    /*
     * Why: each field nobody touched follows the saved profile, so a sync swapping a pending photo for
     * its upload isn't undone by a save of the name; an edited field keeps the edit.
     */
    private fun onSavedProfileChanged(profile: UserProfile) {
        val previous = saved
        saved = profile
        edit { state ->
            if (previous == null) return@edit state.withProfile(profile).copy(isLoaded = isAccountRead)
            val name = if (state.name.trim() == previous.displayName) profile.displayName else state.name
            val color = if (state.avatarColor == previous.avatarColor) profile.avatarColor else state.avatarColor
            val photo = if (state.photo == previous.photo) profile.photo else state.photo
            state.withName(name).copy(
                avatarColor = color,
                photo = photo,
            )
        }
    }

    private fun discard() {
        val profile = saved ?: return
        trackEvent("edit_profile_discard_clicked", emptyMap())
        photoPicker.cancel()
        uiState.update {
            it.withProfile(profile).copy(
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    // Why: the account's picture is already a public URL, so nothing is prepared or uploaded, as in the profile setup.
    private fun useAccountPhoto() {
        val url = uiState.value.accountPhotoUrl ?: return
        if (uiState.value.isSaving) return
        trackEvent("account_photo_selected", mapOf("source" to EDIT_PROFILE_SOURCE))
        photoPicker.cancel()
        edit {
            it.copy(
                photo = ProfilePhoto.Remote(url),
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    // Why: with no photo there is nothing to remove.
    private fun removePhoto() {
        if (uiState.value.isSaving || uiState.value.photo == null) return
        trackEvent("photo_removed", mapOf("source" to EDIT_PROFILE_SOURCE))
        photoPicker.cancel()
        edit {
            it.copy(
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    private fun showPhotoCrop(editor: PhotoCropEditor?) {
        uiState.update { it.copy(photoCrop = editor) }
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
        edit {
            it.copy(
                photo = photo,
                isPreparingPhoto = false,
            )
        }
    }

    /*
     * Why: Android and iOS open the editor over the profile (a sheet, a dialog) and leave it once saved; the web and
     * the Mac draw it inside the profile, where it stays with the saved values.
     */
    private fun save() {
        if (!uiState.value.canSave) return
        uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val state = uiState.value
            val profile = UserProfile(
                displayName = state.name.trim(),
                avatarColor = state.avatarColor,
                photo = state.photo,
            )
            saveProfile(profile)
            trackEvent("profile_saved", mapOf("has_photo" to (profile.photo != null)))
            saved = profile
            // Why: what was typed while saving stays, compared with what was just saved.
            edit { it.copy(isSaving = false) }
            // Why: saved, so leaving asks nothing; said now, since the state's own report reaches the stack later.
            navigator.markUnsaved(Route.EditProfile, false)
            navigator.navigateBackFrom(Route.EditProfile)
        }
    }

    private fun edit(transform: (EditProfileUiState) -> EditProfileUiState) {
        uiState.update { state ->
            val edited = transform(state)
            edited.copy(hasChanges = edited.isDifferentFrom(saved))
        }
    }

    private fun EditProfileUiState.withProfile(profile: UserProfile): EditProfileUiState = withName(profile.displayName)
        .copy(
            avatarColor = profile.avatarColor,
            photo = profile.photo,
            hasChanges = false,
        )

    private fun EditProfileUiState.withName(name: String): EditProfileUiState {
        val validation = validateDisplayName(name)
        return copy(
            name = name,
            nameError = nameErrorOf(name, validation),
            isNameValid = validation == DisplayNameValidation.VALID,
        )
    }

    private fun EditProfileUiState.isDifferentFrom(profile: UserProfile?): Boolean = profile == null ||
        name.trim() != profile.displayName ||
        avatarColor != profile.avatarColor ||
        photo != profile.photo

    // Why: a name over the limit stays as typed with an error, so a pasted name is never cut unseen.
    private fun nameErrorOf(
        name: String,
        validation: DisplayNameValidation,
    ): EditProfileNameError? = when {
        validation == DisplayNameValidation.TOO_LONG -> EditProfileNameError.TOO_LONG
        validation == DisplayNameValidation.TOO_SHORT && name.isNotBlank() -> EditProfileNameError.TOO_SHORT
        else -> null
    }
}

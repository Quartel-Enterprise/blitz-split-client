package com.quare.blitzsplit.feature.editprofile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.DisplayNameValidation
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ValidateDisplayName
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileNameError
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    prepareProfilePhoto: PrepareProfilePhoto,
    observeProfile: ObserveProfile,
    getSessionUser: GetSessionUser,
    observeAccount: ObserveAccount,
) : ViewModel() {
    val uiState: StateFlow<EditProfileUiState>
        field = MutableStateFlow(
            EditProfileUiState(
                name = "",
                avatarColor = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
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

    private val photoPicker = ProfilePhotoPicker(
        prepareProfilePhoto = prepareProfilePhoto,
        scope = viewModelScope,
        showStatus = ::showPhotoStatus,
        onPrepared = ::showPhoto,
    )

    val stateWatcher: FlowWatcher<EditProfileUiState> = FlowWatcher(uiState)

    init {
        observeProfile()
            .filterNotNull()
            .onEach(::onSavedProfileChanged)
            .launchIn(viewModelScope)
        /*
         * Why: keyed on the user too, so another account of the same provider reads its own picture; and a newer
         * status drops a session read still running, so the notice never waits on it.
         */
        viewModelScope.launch {
            observeAccount()
                .map { account -> account.status as? AccountStatus.Connected }
                .distinctUntilChanged()
                .collectLatest { connected ->
                    // Why: the notice names the provider at once; the picture follows once the session is read.
                    uiState.update {
                        it.copy(
                            accountProvider = connected?.provider,
                            accountPhotoUrl = null,
                        )
                    }
                    // Why: only a connected account has a picture of its own to offer.
                    val accountPhotoUrl = connected?.let { getSessionUser()?.photoUrl }
                    uiState.update { it.copy(accountPhotoUrl = accountPhotoUrl) }
                }
        }
    }

    fun onEvent(event: EditProfileUiEvent) {
        when (event) {
            is EditProfileUiEvent.OnNameChanged -> edit { it.withName(event.name) }
            is EditProfileUiEvent.OnAvatarColorSelected -> edit { it.copy(avatarColor = event.color) }
            is EditProfileUiEvent.OnPhotoPicked -> preparePhoto(event.source)
            EditProfileUiEvent.OnPhotoUnreadable -> photoPicker.fail()
            EditProfileUiEvent.OnAccountPhotoSelected -> useAccountPhoto()
            EditProfileUiEvent.OnPhotoRemoved -> removePhoto()
            EditProfileUiEvent.OnSaveClicked -> save()
            EditProfileUiEvent.OnDiscardClicked -> discard()
            EditProfileUiEvent.OnBackClicked -> navigator.navigateBack()
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
            if (previous == null) return@edit state.withProfile(profile)
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
        photoPicker.cancel()
        edit {
            it.copy(
                photo = ProfilePhoto.Remote(url),
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    private fun removePhoto() {
        if (uiState.value.isSaving) return
        photoPicker.cancel()
        edit {
            it.copy(
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    // Why: Save sends the photo shown, so a pick while saving is ignored.
    private fun preparePhoto(source: ByteArray) {
        if (uiState.value.isSaving) return
        photoPicker.prepare(source)
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
     * Why: the phones push the editor and leave it once saved; the web and the Mac draw it inside the
     * profile, where it stays with the saved values.
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
            saved = profile
            // Why: what was typed while saving stays, compared with what was just saved.
            edit { it.copy(isSaving = false) }
            if (navigator.backStack.value.lastOrNull() == Route.EditProfile) navigator.navigateBack()
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

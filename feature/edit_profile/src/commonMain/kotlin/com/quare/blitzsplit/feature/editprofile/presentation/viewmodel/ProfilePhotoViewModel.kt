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
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.photo.InstantPhotoSaver
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.PROFILE_PHOTO_SOURCE
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
 * Why: the profile's avatar opened big (Route.ProfilePhoto), as Bible Planner's: its shortcuts change the photo at
 * once, saved over the profile as it is now, offline too, and the viewer stays open showing the new one. A picked
 * photo is framed in "Ajustar foto" first, as in "Editar perfil".
 */
class ProfilePhotoViewModel(
    private val saveProfile: SaveProfile,
    private val navigator: Navigator,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    observeProfile: ObserveProfile,
    observeAccountPhoto: ObserveAccountPhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<ProfilePhotoUiEvent>(trackEvent) {
    val uiState: StateFlow<ProfilePhotoUiState>
        field = MutableStateFlow(
            ProfilePhotoUiState(
                isLoaded = false,
                displayName = "",
                avatarColor = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                accountProvider = null,
                accountPhotoUrl = null,
            ),
        )

    val stateWatcher: FlowWatcher<ProfilePhotoUiState> = FlowWatcher(uiState)

    private var saved: UserProfile? = null

    private val photoSaver = InstantPhotoSaver(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        showStatus = ::showPhotoStatus,
        showEditor = ::showPhotoCrop,
        showPhoto = ::showPhoto,
        save = ::saveProfilePhoto,
    )

    init {
        observeProfile()
            .filterNotNull()
            .onEach { profile ->
                saved = profile
                uiState.update {
                    it.copy(
                        isLoaded = true,
                        displayName = profile.displayName,
                        avatarColor = profile.avatarColor,
                        photo = profile.photo,
                    )
                }
            }.launchIn(viewModelScope)
        observeAccountPhoto()
            .onEach { account ->
                uiState.update {
                    it.copy(
                        accountProvider = account.provider,
                        accountPhotoUrl = account.photoUrl,
                    )
                }
            }.launchIn(viewModelScope)
    }

    // Why: a shortcut saves over the saved profile, so nothing is saved before it was read.
    override fun isAccepted(event: ProfilePhotoUiEvent): Boolean = when {
        event == ProfilePhotoUiEvent.OnDismissed -> true

        !uiState.value.isLoaded -> false

        // Why: the picker drops a pick and its Done while a photo saves, so neither counts then.
        photoSaver.isSaving -> !event.isDroppedWhileSaving()

        else -> true
    }

    private fun ProfilePhotoUiEvent.isDroppedWhileSaving(): Boolean = this is ProfilePhotoUiEvent.OnPhotoPicked ||
        this == ProfilePhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked)

    override fun handleEvent(event: ProfilePhotoUiEvent) {
        when (event) {
            is ProfilePhotoUiEvent.OnPhotoPicked -> photoSaver.prepare(event.source)

            is ProfilePhotoUiEvent.OnPhotoCrop -> photoSaver.onEvent(event.event)

            ProfilePhotoUiEvent.OnPhotoUnreadable -> photoSaver.fail()

            ProfilePhotoUiEvent.OnAccountPhotoSelected -> useAccountPhoto()

            ProfilePhotoUiEvent.OnPhotoRemoved -> if (uiState.value.photo != null && photoSaver.replace(null)) {
                trackEvent("photo_removed", mapOf("source" to PROFILE_PHOTO_SOURCE))
            }

            // Why: the backdrop and the close button both dismiss it, and only the first closes it.
            ProfilePhotoUiEvent.OnDismissed -> navigator.navigateBackFrom(Route.ProfilePhoto)
        }
    }

    // Why: the account's picture is already a public URL, so nothing is prepared or uploaded.
    private fun useAccountPhoto() {
        val url = uiState.value.accountPhotoUrl ?: return
        if (photoSaver.replace(ProfilePhoto.Remote(url))) {
            trackEvent("account_photo_selected", mapOf("source" to PROFILE_PHOTO_SOURCE))
        }
    }

    private fun showPhoto(photo: ProfilePhoto?) {
        uiState.update {
            it.copy(
                photo = photo,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
    }

    private suspend fun saveProfilePhoto(photo: ProfilePhoto?) {
        val profile = saved ?: return
        saveProfile(profile.copy(photo = photo))
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
}

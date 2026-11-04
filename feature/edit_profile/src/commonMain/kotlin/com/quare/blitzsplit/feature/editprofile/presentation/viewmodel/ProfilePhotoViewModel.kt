package com.quare.blitzsplit.feature.editprofile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountPhoto
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.profile.photo.ProfilePhotoPicker
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
) : ViewModel() {
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

    private var isSaving = false

    /*
     * Why: a photo chosen is framed, prepared and saved even when the viewer closes meanwhile (✕, back, Esc), which
     * clears viewModelScope with the entry; only the state nobody draws any more is updated in vain.
     */
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = saveScope,
        showStatus = ::showPhotoStatus,
        showEditor = ::showPhotoCrop,
        onPrepared = ::savePhoto,
        isBusy = { isSaving },
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

    fun onEvent(event: ProfilePhotoUiEvent) {
        // Why: a shortcut saves over the saved profile, so nothing is saved before it was read.
        if (!uiState.value.isLoaded && event != ProfilePhotoUiEvent.OnDismissed) return
        when (event) {
            is ProfilePhotoUiEvent.OnPhotoPicked -> photoPicker.prepare(event.source)
            is ProfilePhotoUiEvent.OnPhotoCrop -> photoPicker.onEvent(event.event)
            ProfilePhotoUiEvent.OnPhotoUnreadable -> photoPicker.fail()
            ProfilePhotoUiEvent.OnAccountPhotoSelected -> useAccountPhoto()
            ProfilePhotoUiEvent.OnPhotoRemoved -> replacePhoto(null)
            ProfilePhotoUiEvent.OnDismissed -> navigator.navigateBack()
        }
    }

    // Why: the account's picture is already a public URL, so nothing is prepared or uploaded.
    private fun useAccountPhoto() {
        val url = uiState.value.accountPhotoUrl ?: return
        replacePhoto(ProfilePhoto.Remote(url))
    }

    // Why: whatever replaces the photo wins over a pick still being read or framed.
    private fun replacePhoto(photo: ProfilePhoto?) {
        if (isSaving) return
        photoPicker.cancel()
        savePhoto(photo)
    }

    private fun savePhoto(photo: ProfilePhoto?) {
        val profile = saved ?: return
        isSaving = true
        uiState.update {
            it.copy(
                photo = photo,
                isPreparingPhoto = false,
                showsPhotoError = false,
            )
        }
        saveScope.launch {
            saveProfile(profile.copy(photo = photo))
            isSaving = false
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
}

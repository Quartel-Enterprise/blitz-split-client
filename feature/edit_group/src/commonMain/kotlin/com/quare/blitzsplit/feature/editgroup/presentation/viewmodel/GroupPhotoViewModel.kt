package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroupPhoto
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import com.quare.blitzsplit.core.profile.photo.InstantPhotoSaver
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.GROUP_PHOTO_SOURCE
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
 * Why: the group's photo opened big (Route.GroupPhoto), as the profile's: its shortcuts change the photo at once,
 * saved over the group as it is now, offline too, and the viewer stays open showing the new one. A picked photo is
 * framed in "Ajustar foto" first, as in "Editar grupo".
 */
class GroupPhotoViewModel(
    private val route: Route.GroupPhoto,
    private val navigator: Navigator,
    private val updateGroupPhoto: UpdateGroupPhoto,
    observeGroup: ObserveGroup,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    trackEvent: TrackEvent,
) : TrackedViewModel<GroupPhotoUiEvent>(trackEvent) {
    val uiState: StateFlow<GroupPhotoUiState>
        field = MutableStateFlow(
            GroupPhotoUiState(
                isLoaded = false,
                name = "",
                initials = "",
                color = AvatarColor.default,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
            ),
        )

    val stateWatcher: FlowWatcher<GroupPhotoUiState> = FlowWatcher(uiState)

    private var saved: Group? = null

    private val photoSaver = InstantPhotoSaver(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        showStatus = ::showPhotoStatus,
        showEditor = ::showPhotoCrop,
        showPhoto = ::showPhoto,
        save = ::saveGroupPhoto,
    )

    init {
        observeGroup(route.groupId)
            .onEach { group ->
                saved = group
                // Why: the group went (a sign out, an account switch) while its photo was open.
                if (group == null) {
                    dismiss()
                } else {
                    uiState.update {
                        it.copy(
                            isLoaded = true,
                            name = group.name,
                            initials = Initials.of(group.name),
                            color = group.color,
                            photo = group.photo,
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    /*
     * Why: a shortcut saves over the saved group, so nothing is saved before it was read, nor once it went while the
     * viewer still shows (its dismissal on the way).
     */
    override fun isAccepted(event: GroupPhotoUiEvent): Boolean = when {
        event == GroupPhotoUiEvent.OnDismissed -> true

        saved == null -> false

        // Why: the picker drops a pick and its Done while a photo saves, so neither counts then.
        photoSaver.isSaving -> !event.isDroppedWhileSaving()

        else -> true
    }

    private fun GroupPhotoUiEvent.isDroppedWhileSaving(): Boolean = this is GroupPhotoUiEvent.OnPhotoPicked ||
        this == GroupPhotoUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked)

    override fun handleEvent(event: GroupPhotoUiEvent) {
        when (event) {
            is GroupPhotoUiEvent.OnPhotoPicked -> photoSaver.prepare(event.source)

            is GroupPhotoUiEvent.OnPhotoCrop -> photoSaver.onEvent(event.event)

            GroupPhotoUiEvent.OnPhotoUnreadable -> photoSaver.fail()

            GroupPhotoUiEvent.OnPhotoRemoved -> if (uiState.value.photo != null && photoSaver.replace(null)) {
                trackEvent("photo_removed", mapOf("source" to GROUP_PHOTO_SOURCE))
            }

            // Why: the backdrop and the close button both dismiss it, and only the first closes it.
            GroupPhotoUiEvent.OnDismissed -> dismiss()
        }
    }

    private fun dismiss() {
        navigator.navigateBackFrom(route)
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

    private suspend fun saveGroupPhoto(photo: ProfilePhoto?) {
        val group = saved ?: return
        // Why: only the photo is written, so another member's rename or a newer app's type and color stay untouched.
        updateGroupPhoto(
            groupId = route.groupId,
            photo = photo,
        ).onFailure {
            // Why: Room kept the old photo and won't emit again, so the viewer shows the one that is saved.
            showPhoto(group.photo)
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

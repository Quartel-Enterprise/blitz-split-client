@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsEditProfileState(
    val name: String,
    val nameLength: Int,
    val avatarColor: String,
    val avatarColors: Array<String>,
    val avatarInitial: String,
    val photoUrl: String?,
    val hasPhoto: Boolean,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: null while the name is valid; otherwise the EditProfileNameError's name.
    val nameError: String?,
    val nameMinLength: Int,
    val nameMaxLength: Int,
    val hasChanges: Boolean,
    val canSave: Boolean,
    val isSaving: Boolean,
    // Why: "google" or "apple" when that account gave the name and photo.
    val accountProvider: String?,
)

class EditProfilePresenter {
    private val holder = ViewModelHolder(EditProfileViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsEditProfileState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsEditProfileState(
                name = state.name,
                nameLength = state.nameLength,
                avatarColor = state.avatarColor.code,
                avatarColors = state.avatarColors.map(AvatarColor::code).toTypedArray(),
                avatarInitial = state.avatarInitial,
                photoUrl = state.photoUrl,
                hasPhoto = state.hasPhoto,
                isPreparingPhoto = state.isPreparingPhoto,
                showsPhotoError = state.showsPhotoError,
                nameError = state.nameError?.name,
                nameMinLength = state.nameMinLength,
                nameMaxLength = state.nameMaxLength,
                hasChanges = state.hasChanges,
                canSave = state.canSave,
                isSaving = state.isSaving,
                accountProvider = state.accountProvider?.code,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun changeName(name: String) {
        viewModel.onEvent(EditProfileUiEvent.OnNameChanged(name))
    }

    fun selectAvatarColor(colorCode: String) {
        val color = AvatarColor.fromCodeOrNull(colorCode) ?: return
        viewModel.onEvent(EditProfileUiEvent.OnAvatarColorSelected(color))
    }

    // Why: a Kotlin ByteArray is an Int8Array in JS, so the file's bytes cross without a copy.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)
    }

    fun removePhoto() {
        viewModel.onEvent(EditProfileUiEvent.OnPhotoRemoved)
    }

    fun save() {
        viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
    }

    fun discard() {
        viewModel.onEvent(EditProfileUiEvent.OnDiscardClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

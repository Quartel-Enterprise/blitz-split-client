@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsProfileSetupState(
    val name: String,
    val avatarColor: String,
    val avatarColors: Array<String>,
    val avatarInitial: String,
    val photoUrl: String?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: null while the name is valid; otherwise the NameError's name.
    val nameError: String?,
    val nameMinLength: Int,
    val nameMaxLength: Int,
    val canContinue: Boolean,
    val isSaving: Boolean,
)

class ProfileSetupPresenter {
    private val holder = ViewModelHolder(ProfileSetupViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsProfileSetupState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsProfileSetupState(
                name = state.name,
                avatarColor = state.avatarColor.code,
                avatarColors = state.avatarColors.map(AvatarColor::code).toTypedArray(),
                avatarInitial = state.avatarInitial,
                photoUrl = state.photoUrl,
                isPreparingPhoto = state.isPreparingPhoto,
                showsPhotoError = state.showsPhotoError,
                nameError = state.nameError?.name,
                nameMinLength = state.nameMinLength,
                nameMaxLength = state.nameMaxLength,
                canContinue = state.canContinue,
                isSaving = state.isSaving,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun changeName(name: String) {
        viewModel.onEvent(ProfileSetupUiEvent.OnNameChanged(name))
    }

    fun selectAvatarColor(colorCode: String) {
        val color = AvatarColor.fromCodeOrNull(colorCode) ?: return
        viewModel.onEvent(ProfileSetupUiEvent.OnAvatarColorSelected(color))
    }

    // Why: a Kotlin ByteArray is an Int8Array in JS, so the file's bytes cross without a copy.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(ProfileSetupUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(ProfileSetupUiEvent.OnPhotoUnreadable)
    }

    fun continueClicked() {
        viewModel.onEvent(ProfileSetupUiEvent.OnContinueClicked)
    }

    fun back() {
        viewModel.onEvent(ProfileSetupUiEvent.OnBackClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

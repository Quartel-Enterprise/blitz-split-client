@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.ProfilePhotoViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsProfilePhotoState(
    // Why: false until the saved profile was read: no avatar is drawn before it.
    val isLoaded: Boolean,
    val avatarColor: String,
    val avatarInitial: String,
    val photoUrl: String?,
    val hasPhoto: Boolean,
    // Why: "google" or "apple" while that account's picture is offered as the photo, otherwise null.
    val accountPhotoProvider: String?,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, null while closed.
    val photoCrop: JsPhotoCropState?,
)

// Why: the profile's photo big (Route.ProfilePhoto), whose shortcuts save the photo at once.
class ProfilePhotoPresenter {
    private val holder = ViewModelHolder(ProfilePhotoViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    val crop: PhotoCropPresenter =
        PhotoCropPresenter { event -> viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(event)) }

    fun observeState(onChange: (JsProfilePhotoState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsProfilePhotoState(
                isLoaded = state.isLoaded,
                avatarColor = state.avatarColor.code,
                avatarInitial = state.avatarInitial,
                photoUrl = state.photoUrl,
                hasPhoto = state.hasPhoto,
                accountPhotoProvider = state.accountPhotoProvider?.code,
                isPreparingPhoto = state.isPreparingPhoto,
                showsPhotoError = state.showsPhotoError,
                photoCrop = state.photoCrop?.toJs(),
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: a Kotlin ByteArray is an Int8Array in JS, so the file's bytes cross without a copy.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable)
    }

    fun useAccountPhoto() {
        viewModel.onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected)
    }

    fun removePhoto() {
        viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoRemoved)
    }

    fun dismiss() {
        viewModel.onEvent(ProfilePhotoUiEvent.OnDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

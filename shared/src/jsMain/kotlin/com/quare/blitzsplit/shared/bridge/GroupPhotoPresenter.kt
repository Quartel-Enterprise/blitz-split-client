@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.GroupPhotoViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsGroupPhotoState(
    // Why: false until the group was read: no avatar is drawn before it.
    val isLoaded: Boolean,
    val avatarColor: String,
    val initials: String,
    val photoUrl: String?,
    val hasPhoto: Boolean,
    val isPreparingPhoto: Boolean,
    val showsPhotoError: Boolean,
    // Why: "Ajustar foto" while a picked photo is being framed, null while closed.
    val photoCrop: JsPhotoCropState?,
)

// Why: the group's photo big (Route.GroupPhoto), whose shortcuts save the photo at once.
class GroupPhotoPresenter(
    route: JsRoute,
) {
    private val photoRoute = route.route as Route.GroupPhoto
    private val holder = ViewModelHolder(GroupPhotoViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(photoRoute) }
    }
    private val viewModel get() = holder.viewModel

    val crop: PhotoCropPresenter =
        PhotoCropPresenter { event -> viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(event)) }

    fun observeState(onChange: (JsGroupPhotoState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGroupPhotoState(
                isLoaded = state.isLoaded,
                avatarColor = state.color.code,
                initials = state.initials,
                photoUrl = state.photoUrl,
                hasPhoto = state.hasPhoto,
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
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable)
    }

    fun removePhoto() {
        viewModel.onEvent(GroupPhotoUiEvent.OnPhotoRemoved)
    }

    fun dismiss() {
        viewModel.onEvent(GroupPhotoUiEvent.OnDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

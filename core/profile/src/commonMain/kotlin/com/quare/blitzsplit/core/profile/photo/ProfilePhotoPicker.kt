package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/*
 * Why: the profile setup, "Editar perfil", "Novo grupo" and "Editar grupo" take a picked photo the same way: it opens
 * in "Ajustar foto", and only "Pronto" prepares the square framed there. A newer pick replaces the one still being
 * read or prepared, and a failed one keeps the photo shown. The screen keeps the spinner, the error, the editor and
 * the photo in its own state, through these callbacks. While the screen is busy (saving, creating), a pick and "Pronto"
 * are ignored, since what it sends is the photo shown.
 */
class ProfilePhotoPicker(
    private val loadCropPhoto: LoadCropPhoto,
    private val prepareProfilePhoto: PrepareProfilePhoto,
    private val scope: CoroutineScope,
    private val showStatus: (isPreparing: Boolean, hasFailed: Boolean) -> Unit,
    private val showEditor: (PhotoCropEditor?) -> Unit,
    private val onPrepared: (ProfilePhoto) -> Unit,
    private val isBusy: () -> Boolean = { false },
) {
    private var job: Job? = null

    private var editor: PhotoCropEditor? = null

    // Why: what the profile setup's Continue waits for, so the profile isn't saved without the photo just framed.
    val pickInProgress: Job?
        get() = job?.takeIf { it.isActive }

    fun prepare(source: ByteArray) {
        if (isBusy()) return
        cancel()
        showStatus(true, false)
        job = scope.launch {
            val result = loadCropPhoto(source)
            ensureActive()
            result
                .onSuccess { photo ->
                    showStatus(false, false)
                    edit(
                        PhotoCropEditor(
                            photo = photo,
                            crop = PhotoCrop.of(photo.width, photo.height),
                        ),
                    )
                }.onFailure { showStatus(false, true) }
        }
    }

    fun onEvent(event: PhotoCropEvent) {
        when (event) {
            is PhotoCropEvent.OnDragged -> editCrop { it.toDragged(event.dx, event.dy) }
            is PhotoCropEvent.OnPinched -> editCrop { it.toZoomedBy(event.zoomChange, event.focusX, event.focusY) }
            is PhotoCropEvent.OnZoomChanged -> editCrop { it.toZoomed(event.zoom) }
            PhotoCropEvent.OnTurnClicked -> editCrop(PhotoCrop::toTurned)
            PhotoCropEvent.OnMirrorHorizontallyClicked -> editCrop(PhotoCrop::toMirroredHorizontally)
            PhotoCropEvent.OnMirrorVerticallyClicked -> editCrop(PhotoCrop::toMirroredVertically)
            PhotoCropEvent.OnConfirmClicked -> confirm()
            PhotoCropEvent.OnDismissed -> edit(null)
        }
    }

    // Why: the picker's last answer wins, so a pick still being read stops instead of replacing the error.
    fun fail() {
        cancel()
        showStatus(false, true)
    }

    // Why: also closes the editor, since whatever replaced the photo (a color, the account's picture) wins over it.
    fun cancel() {
        job?.cancel()
        if (editor != null) edit(null)
    }

    // Why: the editor closes at once and the avatar shows the spinner while the square is encoded, as a pick did.
    private fun confirm() {
        if (isBusy()) return
        val confirmed = editor ?: return
        edit(null)
        showStatus(true, false)
        job = scope.launch {
            val result = prepareProfilePhoto(confirmed.photo, confirmed.crop)
            ensureActive()
            result
                .onSuccess(onPrepared)
                .onFailure { showStatus(false, true) }
        }
    }

    private fun editCrop(transform: (PhotoCrop) -> PhotoCrop) {
        val current = editor ?: return
        edit(current.copy(crop = transform(current.crop)))
    }

    private fun edit(editor: PhotoCropEditor?) {
        this.editor = editor
        showEditor(editor)
    }
}

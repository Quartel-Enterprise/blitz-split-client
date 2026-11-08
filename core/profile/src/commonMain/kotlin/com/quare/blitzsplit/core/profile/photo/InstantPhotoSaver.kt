package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/*
 * Why: the profile's photo and a group's photo, opened big, change the same way: a shortcut saves the photo at once,
 * offline too, and the viewer stays open showing the new one; a picked photo is framed in "Ajustar foto" first. One
 * photo saves at a time: a pick, a "Pronto" or a shortcut that comes while one saves is dropped.
 */
class InstantPhotoSaver(
    // Why: the viewer shows the photo chosen before it is saved.
    private val showPhoto: (ProfilePhoto?) -> Unit,
    private val save: suspend (ProfilePhoto?) -> Unit,
    loadCropPhoto: LoadCropPhoto,
    prepareProfilePhoto: PrepareProfilePhoto,
    showStatus: (isPreparing: Boolean, hasFailed: Boolean) -> Unit,
    showEditor: (PhotoCropEditor?) -> Unit,
) {
    var isSaving = false
        private set

    /*
     * Why: a photo chosen is framed, prepared and saved even when the viewer closes meanwhile (✕, back, Esc), which
     * clears viewModelScope with the entry; only the state nobody draws any more is updated in vain.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val photoPicker = ProfilePhotoPicker(
        loadCropPhoto = loadCropPhoto,
        prepareProfilePhoto = prepareProfilePhoto,
        scope = scope,
        showStatus = showStatus,
        showEditor = showEditor,
        onPrepared = ::saveNow,
        isBusy = { isSaving },
    )

    fun prepare(source: ByteArray) {
        photoPicker.prepare(source)
    }

    fun onEvent(event: PhotoCropEvent) {
        photoPicker.onEvent(event)
    }

    fun fail() {
        photoPicker.fail()
    }

    /*
     * Why: whatever replaces the photo wins over a pick still being read or framed. It says whether it took the
     * photo, which Analytics counts: one saving drops any other.
     */
    fun replace(photo: ProfilePhoto?): Boolean {
        if (isSaving) return false
        photoPicker.cancel()
        saveNow(photo)
        return true
    }

    private fun saveNow(photo: ProfilePhoto?) {
        isSaving = true
        showPhoto(photo)
        scope.launch {
            try {
                save(photo)
            } finally {
                isSaving = false
            }
        }
    }
}

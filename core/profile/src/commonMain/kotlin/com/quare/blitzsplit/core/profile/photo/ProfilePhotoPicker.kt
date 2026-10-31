package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/*
 * Why: the profile setup and "Editar perfil" prepare a picked photo the same way: a newer pick replaces the one
 * still being prepared, and a failed one keeps the photo shown. The screen keeps the spinner, the error and the
 * photo in its own state, through these callbacks, and decides when a pick is ignored (while saving).
 */
class ProfilePhotoPicker(
    private val prepareProfilePhoto: PrepareProfilePhoto,
    private val scope: CoroutineScope,
    private val showStatus: (isPreparing: Boolean, hasFailed: Boolean) -> Unit,
    private val onPrepared: (ProfilePhoto) -> Unit,
) {
    private var job: Job? = null

    // Why: what the profile setup's Continue waits for, so the profile isn't saved without the photo just picked.
    val pickInProgress: Job?
        get() = job?.takeIf { it.isActive }

    fun prepare(source: ByteArray) {
        job?.cancel()
        showStatus(true, false)
        job = scope.launch {
            val result = prepareProfilePhoto(source)
            ensureActive()
            result
                .onSuccess(onPrepared)
                .onFailure { showStatus(false, true) }
        }
    }

    // Why: the picker's last answer wins, so a pick still being prepared stops instead of replacing the error.
    fun fail() {
        job?.cancel()
        showStatus(false, true)
    }

    fun cancel() {
        job?.cancel()
    }
}

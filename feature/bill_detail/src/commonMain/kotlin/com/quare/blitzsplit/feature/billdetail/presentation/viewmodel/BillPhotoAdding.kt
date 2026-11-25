package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.usecase.AttachBillImage
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.model.PhotoTooLargeException
import com.quare.blitzsplit.core.profile.domain.usecase.LoadCropPhoto
import com.quare.blitzsplit.core.profile.domain.usecase.PrepareBillPhoto
import com.quare.blitzsplit.core.provider.image.ImageTooLargeException
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Adicionar foto" (Anexos): the photo picked or taken opens in "Ajustar foto", framed in the tile's rounded
 * square, and only "Pronto" attaches the square framed there, on the device first, offline too, by this device's
 * member; "Cancelar" attaches nothing (beta feedback of 2026-11-16). The tile spins while the photo is read and while
 * the square is kept, and takes no other pick meanwhile.
 */
internal class BillPhotoAdding(
    private val route: Route.BillDetail,
    private val uiState: MutableStateFlow<BillDetailUiState>,
    private val scope: CoroutineScope,
    private val loadCropPhoto: LoadCropPhoto,
    private val prepareBillPhoto: PrepareBillPhoto,
    private val attachBillImage: AttachBillImage,
    private val myMemberId: () -> String?,
    private val emit: suspend (BillDetailUiAction) -> Unit,
) {
    /*
     * Why: a square confirmed is encoded and kept even when Conta closes right after "Pronto" (back), which clears
     * viewModelScope with the entry; only the state nobody draws any more is updated in vain.
     */
    private val attachScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Why: whether it took the pick, which Analytics counts.
    fun pick(source: ByteArray): Boolean {
        if (!uiState.value.canAddPhoto) return false
        uiState.update { it.copy(isAddingPhoto = true) }
        scope.launch {
            loadCropPhoto(source)
                .onSuccess { photo ->
                    val editor = PhotoCropEditor(
                        photo = photo,
                        // Why: a long receipt zooms out until it fits whole in the square (feedback of 2026-11-25).
                        crop = PhotoCrop.of(photo.width, photo.height, canFit = true),
                    )
                    // Why: a bill whose deletion started while the photo was read takes no photo; no editor opens.
                    uiState.update { state ->
                        state.copy(
                            isAddingPhoto = false,
                            photoCrop = editor.takeIf { state.areActionsEnabled },
                        )
                    }
                }.onFailure { error -> finish(errorOf(error)) }
        }
        return true
    }

    fun onCropEvent(event: PhotoCropEvent) {
        val editor = uiState.value.photoCrop ?: return
        when (event) {
            PhotoCropEvent.OnConfirmClicked -> attach(editor)
            PhotoCropEvent.OnDismissed -> uiState.update { it.copy(photoCrop = null) }
            else -> uiState.update { it.copy(photoCrop = editor.toEdited(event)) }
        }
    }

    // Why: the editor closes at once and the tile spins while the square is encoded and kept.
    private fun attach(editor: PhotoCropEditor) {
        uiState.update {
            it.copy(
                photoCrop = null,
                isAddingPhoto = true,
            )
        }
        attachScope.launch {
            val error = prepareBillPhoto(editor.photo, editor.crop).fold(
                onSuccess = { image ->
                    attachBillImage(
                        expenseId = route.expenseId,
                        groupId = route.groupId,
                        kind = BillAttachmentKind.PHOTO,
                        image = image,
                        addedByMemberId = myMemberId(),
                    ).fold(
                        onSuccess = ::errorOf,
                        onFailure = { BillPhotoError.FAILED },
                    )
                },
                onFailure = ::errorOf,
            )
            finish(error)
        }
    }

    private suspend fun finish(error: BillPhotoError?) {
        uiState.update { it.copy(isAddingPhoto = false) }
        error?.let { emit(BillDetailUiAction.ShowPhotoError(it)) }
    }

    private fun errorOf(addition: BillAttachmentAddition): BillPhotoError? = when (addition) {
        BillAttachmentAddition.ADDED -> null
        BillAttachmentAddition.NO_ROOM -> BillPhotoError.NO_ROOM
    }

    private fun errorOf(error: Throwable): BillPhotoError = when (error) {
        is UnsupportedImageException -> BillPhotoError.UNREADABLE
        is ImageTooLargeException, is PhotoTooLargeException -> BillPhotoError.TOO_LARGE
        else -> BillPhotoError.FAILED
    }
}

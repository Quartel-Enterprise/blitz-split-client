package com.quare.blitzsplit.feature.billdetail.presentation.model

sealed interface BillDetailUiAction {
    // Why: the device couldn't delete the bill, which is rare enough to just say so.
    data object ShowDeleteError : BillDetailUiAction

    // Why: the comment couldn't be kept on the device (its bill went meanwhile); what was typed stays.
    data object ShowCommentError : BillDetailUiAction

    // Why: the edit, the deletion or the like couldn't be kept on the device (the comment went meanwhile).
    data object ShowCommentChangeError : BillDetailUiAction

    // Why: "Esse comentário foi apagado", the comment a link opened at is gone.
    data object ShowCommentGone : BillDetailUiAction

    // Why: each platform writes "Ana comentou em “Jantar”: “…”" in its words, with the link, for the system share.
    data class ShareComment(
        val share: BillCommentShare,
    ) : BillDetailUiAction

    data object ShowUndoError : BillDetailUiAction

    data class ShowPhotoError(
        val error: BillPhotoError,
    ) : BillDetailUiAction

    data object ShowRemovePhotoError : BillDetailUiAction
}

package com.quare.blitzsplit.feature.billdetail.presentation.model

sealed interface SharedBillUiAction {
    // Why: "Não deu para enviar o comentário. Tente de novo."
    data object ShowCommentError : SharedBillUiAction

    // Why: as Conta's: the keyboard closes, and a new comment, the last one, is scrolled to once the page shows it.
    data class ShowPostedComment(
        val isReply: Boolean,
    ) : SharedBillUiAction

    // Why: "Crie seu perfil para comentar.": someone signed in whose name neither the server nor the device knows.
    data object ShowNeedsName : SharedBillUiAction

    // Why: "Não deu para salvar a mudança. Tente de novo.", an edit, a deletion or a like that didn't go.
    data object ShowCommentChangeError : SharedBillUiAction

    // Why: "Esse comentário foi apagado", the comment the link was shared at is gone.
    data object ShowCommentGone : SharedBillUiAction

    data class ShareComment(
        val share: BillCommentShare,
    ) : SharedBillUiAction
}

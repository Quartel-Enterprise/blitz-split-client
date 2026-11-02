package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface BillDetailUiEvent : UiEvent {
    data object OnBackClicked : BillDetailUiEvent

    // Why: "Editar conta" in the top bar.
    data object OnEditClicked : BillDetailUiEvent

    // Why: "Apagar conta" in the top bar, which asks first.
    data object OnDeleteClicked : BillDetailUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : BillDetailUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : BillDetailUiEvent

    data class OnCommentChanged(
        val text: String,
    ) : BillDetailUiEvent

    // Why: "Enviar comentário", offline too: it waits in the queue.
    data object OnSendCommentClicked : BillDetailUiEvent

    // Why: "Responder" under a comment or a reply.
    data class OnReplyClicked(
        val commentId: String,
    ) : BillDetailUiEvent

    // Why: the ✕ of "Respondendo a Diego".
    data object OnReplyCancelled : BillDetailUiEvent

    // Why: "Desfazer" on the history's latest edit.
    data object OnUndoClicked : BillDetailUiEvent
}

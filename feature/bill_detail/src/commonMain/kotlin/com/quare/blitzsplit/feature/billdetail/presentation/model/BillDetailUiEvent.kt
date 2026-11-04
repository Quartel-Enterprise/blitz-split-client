package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface BillDetailUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: typed letter by letter; what was typed shows in the comment sent.
            is OnCommentChanged -> EventAnalytics.NotTracked

            // Why: a close, or a comment a link opened at shown, which nobody tapped.
            OnReplyCancelled,
            OnCommentEditCancelled,
            OnCommentLikesDismissed,
            OnFocusedCommentShown,
            OnViewerDismissed,
            -> EventAnalytics.NotTracked

            // Why: a swipe through the viewer, like a scroll.
            is OnViewerPageChanged -> EventAnalytics.NotTracked

            /*
             * Why: each counts only when the screen takes it (a comment still there and open to it, a question still
             * open), never a late tap, as the ViewModel checks before acting.
             */
            OnSendCommentClicked -> EventAnalytics.Manual("comment_send_clicked")

            is OnReplyClicked -> EventAnalytics.Manual("comment_reply_clicked")

            is OnCommentEditClicked -> EventAnalytics.Manual("comment_edit_clicked")

            is OnCommentDeleteClicked -> EventAnalytics.Manual("comment_delete_clicked")

            OnCommentDeleteConfirmed -> EventAnalytics.Manual("comment_delete_confirmed")

            OnCommentDeleteDismissed -> EventAnalytics.Manual("comment_delete_cancelled")

            is OnCommentLikeClicked -> EventAnalytics.Manual("comment_like_toggled")

            is OnCommentLikesClicked -> EventAnalytics.Manual("comment_likes_clicked")

            is OnCommentShareClicked -> EventAnalytics.Manual("comment_share_clicked")

            is OnAttachmentClicked -> EventAnalytics.Manual("bill_attachment_clicked")

            is OnPersonClicked -> EventAnalytics.Manual("person_clicked")

            OnEditClicked -> EventAnalytics.Manual("bill_edit_clicked")

            OnShareClicked -> EventAnalytics.Manual("bill_share_clicked")

            OnDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

            OnDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

            OnDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")

            OnUndoClicked -> EventAnalytics.Manual("bill_undo_clicked")

            // Why: a photo handed back, which stands for the tap that opened the camera or the picker.
            is OnPhotoPicked -> EventAnalytics.Manual("photo_picked")

            // Why: the platform failing to read the file, which the photo picked already counted.
            OnPhotoUnreadable -> EventAnalytics.NotTracked

            is OnRemoveAttachmentClicked -> EventAnalytics.Manual("bill_attachment_removed")
        }

    // Why: what "Comentários" handles on its own (BillDetailComments).
    sealed interface CommentEvent : BillDetailUiEvent

    data object OnBackClicked : BillDetailUiEvent

    // Why: a person's face or name (the split, who paid, a comment's author, Histórico), which opens their profile.
    data class OnPersonClicked(
        val memberId: String,
    ) : BillDetailUiEvent

    // Why: "Editar conta" in the top bar.
    data object OnEditClicked : BillDetailUiEvent

    // Why: "Compartilhar" in the top bar, which opens "Compartilhar conta".
    data object OnShareClicked : BillDetailUiEvent

    // Why: "Apagar conta" in the top bar, which asks first.
    data object OnDeleteClicked : BillDetailUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : BillDetailUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : BillDetailUiEvent

    data class OnCommentChanged(
        val text: String,
    ) : CommentEvent

    // Why: "Enviar comentário", offline too: it waits in the queue.
    data object OnSendCommentClicked : CommentEvent

    // Why: "Responder" under a comment or a reply.
    data class OnReplyClicked(
        val commentId: String,
    ) : CommentEvent

    // Why: the ✕ of "Respondendo a Diego".
    data object OnReplyCancelled : CommentEvent

    // Why: "Editar" in the menu of the user's own comment, which puts its text in the composer.
    data class OnCommentEditClicked(
        val commentId: String,
    ) : CommentEvent

    // Why: the ✕ of "Editando comentário", which gives the composer back what was typed before.
    data object OnCommentEditCancelled : CommentEvent

    // Why: "Apagar" in the menu of the user's own comment, which asks first.
    data class OnCommentDeleteClicked(
        val commentId: String,
    ) : CommentEvent

    data object OnCommentDeleteConfirmed : CommentEvent

    data object OnCommentDeleteDismissed : CommentEvent

    // Why: the heart, which likes it or undoes the like, offline too.
    data class OnCommentLikeClicked(
        val commentId: String,
    ) : CommentEvent

    // Why: the count beside the heart (a long press or a hover too), which lists who liked it.
    data class OnCommentLikesClicked(
        val commentId: String,
    ) : CommentEvent

    data object OnCommentLikesDismissed : CommentEvent

    // Why: "Compartilhar" in a comment's menu: its link with its text, through the system share.
    data class OnCommentShareClicked(
        val commentId: String,
    ) : CommentEvent

    // Why: the comment a link opened at was scrolled to and highlighted.
    data object OnFocusedCommentShown : CommentEvent

    // Why: "Desfazer" on the history's latest edit.
    data object OnUndoClicked : BillDetailUiEvent

    // Why: the photo taken or picked from the gallery or the files, as the file came.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : BillDetailUiEvent

    // Why: the platform couldn't read the file picked (a cloud photo not downloaded, say).
    data object OnPhotoUnreadable : BillDetailUiEvent

    // Why: a tile of "Anexos", which opens the viewer on it.
    data class OnAttachmentClicked(
        val id: String,
    ) : BillDetailUiEvent

    // Why: swiping the viewer to another attachment.
    data class OnViewerPageChanged(
        val id: String,
    ) : BillDetailUiEvent

    data object OnViewerDismissed : BillDetailUiEvent

    // Why: "Remover foto" in the viewer, offline too and with no question, as the boards draw it.
    data class OnRemoveAttachmentClicked(
        val id: String,
    ) : BillDetailUiEvent
}

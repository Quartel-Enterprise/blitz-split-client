package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface SharedBillUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the comments' taps, as Conta's send them, each once the page takes it.
            is OnCommentLikeClicked -> CommentTap.LIKE.analytics

            is OnCommentLikesClicked -> CommentTap.LIKES.analytics

            is OnCommentShareClicked -> CommentTap.SHARE.analytics

            is OnCommentDeleteClicked -> CommentTap.DELETE.analytics

            OnCommentDeleteConfirmed -> CommentTap.DELETE_CONFIRM.analytics

            OnCommentDeleteDismissed -> CommentTap.DELETE_CANCEL.analytics

            is OnCommentEditClicked -> CommentTap.EDIT.analytics

            OnCommentEditCancelled -> CommentTap.EDIT_CANCEL.analytics

            is OnReplyClicked -> CommentTap.REPLY.analytics

            OnReplyCancelled -> CommentTap.REPLY_CANCEL.analytics

            OnSendCommentClicked -> CommentTap.SEND.analytics

            OnRetryClicked -> EventAnalytics.Manual("retry_clicked")

            OnSignInClicked -> EventAnalytics.Manual("shared_bill_sign_in_clicked")

            OnOpenInAppClicked -> EventAnalytics.Manual("shared_bill_open_in_app_clicked")

            is OnAttachmentClicked -> EventAnalytics.Manual("bill_attachment_clicked")

            /*
             * Why: nobody tapped these to act: a value typed letter by letter (what was typed shows in the comment
             * sent), a swipe through the viewer, a close (the page's back or logo, the viewer or "Curtidas" closed), or
             * a comment a link opened at shown.
             */
            is OnCommentChanged,
            is OnViewerPageChanged,
            OnCloseClicked,
            OnCommentLikesDismissed,
            OnViewerDismissed,
            OnFocusedCommentShown,
            -> EventAnalytics.NotTracked
        }

    data object OnRetryClicked : SharedBillUiEvent

    // Why: "Entrar" and "Entrar para comentar": the welcome without a profile, else the profile's Google or Apple.
    data object OnSignInClicked : SharedBillUiEvent

    // Why: "Abrir no app", the bill's Conta over its group.
    data object OnOpenInAppClicked : SharedBillUiEvent

    // Why: the page's back, or its logo, to the app under it.
    data object OnCloseClicked : SharedBillUiEvent

    data class OnCommentChanged(
        val text: String,
    ) : SharedBillUiEvent

    data object OnSendCommentClicked : SharedBillUiEvent

    data class OnReplyClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data object OnReplyCancelled : SharedBillUiEvent

    // Why: "Editar" in the menu of the caller's own comment, which puts its text in the composer.
    data class OnCommentEditClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data object OnCommentEditCancelled : SharedBillUiEvent

    data class OnCommentDeleteClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data object OnCommentDeleteConfirmed : SharedBillUiEvent

    data object OnCommentDeleteDismissed : SharedBillUiEvent

    data class OnCommentLikeClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data class OnCommentLikesClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data object OnCommentLikesDismissed : SharedBillUiEvent

    // Why: the page's link at the comment, through the system share.
    data class OnCommentShareClicked(
        val commentId: String,
    ) : SharedBillUiEvent

    data object OnFocusedCommentShown : SharedBillUiEvent

    data class OnAttachmentClicked(
        val id: String,
    ) : SharedBillUiEvent

    data class OnViewerPageChanged(
        val id: String,
    ) : SharedBillUiEvent

    data object OnViewerDismissed : SharedBillUiEvent
}

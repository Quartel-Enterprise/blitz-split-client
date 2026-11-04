package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface SharedBillUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the page's back or its logo, which each platform also does its own way.
            OnCloseClicked -> EventAnalytics.NotTracked

            OnRetryClicked -> EventAnalytics.Manual("retry_clicked")

            OnSignInClicked -> EventAnalytics.Manual("shared_bill_sign_in_clicked")

            OnOpenInAppClicked -> EventAnalytics.Manual("shared_bill_open_in_app_clicked")

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

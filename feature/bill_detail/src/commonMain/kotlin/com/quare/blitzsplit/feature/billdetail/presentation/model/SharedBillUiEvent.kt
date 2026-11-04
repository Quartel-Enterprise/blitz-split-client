package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface SharedBillUiEvent : UiEvent {
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

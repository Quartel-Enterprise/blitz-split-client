package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiState
import software.quare.loadable.valueOrNull

// Why: Conta's preview bill as the server sends it to its link's page: nobody is this device's user nor opens a profile.
private fun createPreviewSharedBillState(
    status: SharedBillStatus = SharedBillStatus.OPEN,
    isSignedIn: Boolean = true,
    hasLinkedAccount: Boolean = isSignedIn,
    isOnline: Boolean = true,
    canOpenInApp: Boolean = false,
    isEditingComment: Boolean = false,
): SharedBillUiState {
    val bill = createPreviewBillDetailState(isEditingComment = isEditingComment)
    return SharedBillUiState(
        status = status,
        isOnline = isOnline,
        isSignedIn = isSignedIn,
        hasLinkedAccount = hasLinkedAccount,
        canOpenInApp = canOpenInApp,
        groupName = bill.groupName,
        currency = bill.currency,
        description = bill.description,
        totalMinor = bill.totalMinor,
        spentAt = bill.spentAt,
        spentDay = bill.spentDay,
        categoryId = bill.categoryId,
        categories = bill.categories,
        splitMode = bill.splitMode,
        payers = bill.payers.map { it.copy(opensProfile = false) },
        people = bill.people.filter { it.shareMinor != null }.map {
            it.copy(
                isMe = false,
                hasJoined = true,
                opensProfile = false,
            )
        },
        attachments = bill.attachments.valueOrNull().orEmpty(),
        viewedAttachmentId = null,
        threads = bill.threads.valueOrNull().orEmpty(),
        commentDraft = bill.commentDraft,
        replyTarget = null,
        isSendingComment = false,
        editingCommentId = bill.editingCommentId,
        commentToDelete = null,
        likesCommentId = null,
        focusedCommentId = null,
        isChangingComment = false,
    )
}

@DevicePreviews
@Composable
private fun SharedBillContentPreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(canOpenInApp = true))
}

@DevicePreviews
@Composable
private fun SharedBillContentSignedOutPreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(isSignedIn = false))
}

@DevicePreviews
@Composable
private fun SharedBillContentEditingCommentPreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(isEditingComment = true))
}

@DevicePreviews
@Composable
private fun SharedBillContentLoadingPreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(status = SharedBillStatus.LOADING))
}

@DevicePreviews
@Composable
private fun SharedBillContentNotFoundPreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(status = SharedBillStatus.NOT_FOUND))
}

@DevicePreviews
@Composable
private fun SharedBillContentMembersOnlyPreview() {
    PreviewSharedBillContent(
        createPreviewSharedBillState(
            status = SharedBillStatus.MEMBERS_ONLY,
            isSignedIn = false,
        ),
    )
}

@DevicePreviews
@Composable
private fun SharedBillContentMembersOnlyOutsidePreview() {
    PreviewSharedBillContent(createPreviewSharedBillState(status = SharedBillStatus.MEMBERS_ONLY))
}

@DevicePreviews
@Composable
private fun SharedBillContentFailedOfflinePreview() {
    PreviewSharedBillContent(
        createPreviewSharedBillState(
            status = SharedBillStatus.FAILED,
            isOnline = false,
        ),
    )
}

@Composable
private fun PreviewSharedBillContent(state: SharedBillUiState) {
    BlitzSplitTheme {
        SharedBillContent(
            state = state,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

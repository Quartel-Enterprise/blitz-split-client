package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLink
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.EditBillComment
import com.quare.blitzsplit.core.expenses.domain.usecase.LikeBillComment
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.replyTargetOf
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentShare
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.loadable.valueOrNull

/*
 * Why: Conta's "Comentários" (7.3c to 7.3f, beta feedback 14): writing, answering, editing, deleting, liking and sharing
 * a comment, all on the device first, offline too, by this device's member; the sync takes them up.
 */
internal class BillDetailComments(
    private val route: Route.BillDetail,
    private val uiState: MutableStateFlow<BillDetailUiState>,
    private val scope: CoroutineScope,
    private val addBillComment: AddBillComment,
    private val editBillComment: EditBillComment,
    private val deleteBillComment: DeleteBillComment,
    private val likeBillComment: LikeBillComment,
    private val me: () -> GroupMember?,
    private val emit: suspend (BillDetailUiAction) -> Unit,
) {
    private val edit = CommentComposerEdit()

    // Why: the comment a link opened at is looked for once, when the comments first show.
    private var focusCommentId: String? = route.focusCommentId

    fun onEvent(event: BillDetailUiEvent.CommentEvent) {
        when (event) {
            is BillDetailUiEvent.OnCommentChanged -> uiState.update { it.copy(commentDraft = event.text) }
            BillDetailUiEvent.OnSendCommentClicked -> send()
            is BillDetailUiEvent.OnReplyClicked -> replyTo(event.commentId)
            BillDetailUiEvent.OnReplyCancelled -> uiState.update { it.copy(replyTarget = null) }
            is BillDetailUiEvent.OnCommentEditClicked -> startEditing(event.commentId)
            BillDetailUiEvent.OnCommentEditCancelled -> stopEditing()
            is BillDetailUiEvent.OnCommentDeleteClicked -> askToDelete(event.commentId)
            BillDetailUiEvent.OnCommentDeleteConfirmed -> delete()
            BillDetailUiEvent.OnCommentDeleteDismissed -> uiState.update { it.copy(commentToDelete = null) }
            is BillDetailUiEvent.OnCommentLikeClicked -> toggleLike(event.commentId)
            is BillDetailUiEvent.OnCommentLikesClicked -> showLikes(event.commentId)
            BillDetailUiEvent.OnCommentLikesDismissed -> uiState.update { it.copy(likesCommentId = null) }
            is BillDetailUiEvent.OnCommentShareClicked -> share(event.commentId)
            BillDetailUiEvent.OnFocusedCommentShown -> uiState.update { it.copy(focusedCommentId = null) }
        }
    }

    /*
     * Why: the comments as they show now (threads) and as the device keeps them, tombstones included, which a link's
     * comment is looked for in and an edit under way may lose.
     */
    fun onShown(
        threads: List<BillCommentThread>,
        comments: List<BillComment>,
    ) {
        focusLinkedComment(threads, comments)
        // Why: the comment being edited was deleted on another device meanwhile, so the edit has nothing to save.
        val editingId = uiState.value.editingCommentId
        if (editingId != null && threads.findComment(editingId)?.canChange != true) stopEditing()
        // Why: "Curtidas" closed by itself once the comment or its last like went, so a later like never reopens it.
        if (uiState.value.likesCommentId != null && uiState.value.likesComment == null) {
            uiState.update { it.copy(likesCommentId = null) }
        }
    }

    /*
     * Why: once: highlighted when it shows, "Esse comentário foi apagado" when the device keeps it deleted, and
     * waited for while it isn't on the device yet, which a sync may still bring.
     */
    private fun focusLinkedComment(
        threads: List<BillCommentThread>,
        comments: List<BillComment>,
    ) {
        val commentId = focusCommentId ?: return
        if (threads.findComment(commentId)?.canInteract == true) {
            focusCommentId = null
            uiState.update { it.copy(focusedCommentId = commentId) }
        } else if (comments.any { it.id == commentId }) {
            focusCommentId = null
            scope.launch { emit(BillDetailUiAction.ShowCommentGone) }
        }
    }

    private fun findCommentOrNull(commentId: String): BillCommentRow? = uiState.value.threads
        .valueOrNull()
        ?.findComment(commentId)

    private fun replyTo(commentId: String) {
        val threads = uiState.value.threads.valueOrNull() ?: return
        val target = replyTargetOf(threads, commentId) ?: return
        uiState.update { it.copy(replyTarget = target) }
    }

    private fun send() {
        val state = uiState.value
        if (!state.canSendComment) return
        state.editingCommentId?.let { return saveEdit(it) }
        val me = me()
        val replyTarget = state.replyTarget
        uiState.update {
            it.copy(
                commentDraft = "",
                replyTarget = null,
            )
        }
        scope.launch {
            addBillComment(
                expenseId = route.expenseId,
                groupId = route.groupId,
                body = state.commentDraft,
                threadId = replyTarget?.threadId,
                replyToName = replyTarget?.answeredName,
                authorMemberId = me?.id,
                authorName = me?.name,
            ).onFailure {
                // Why: back in the field only while nothing new was typed there, which would be lost.
                uiState.update {
                    if (it.commentDraft.isEmpty() && it.replyTarget == null) {
                        it.copy(
                            commentDraft = state.commentDraft,
                            replyTarget = replyTarget,
                        )
                    } else {
                        it
                    }
                }
                emit(BillDetailUiAction.ShowCommentError)
            }
        }
    }

    private fun composerOf(state: BillDetailUiState): CommentComposer = CommentComposer(
        editingCommentId = state.editingCommentId,
        draft = state.commentDraft,
        replyTarget = state.replyTarget,
    )

    private fun show(composer: CommentComposer) {
        uiState.update {
            it.copy(
                editingCommentId = composer.editingCommentId,
                commentDraft = composer.draft,
                replyTarget = composer.replyTarget,
            )
        }
    }

    private fun startEditing(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canChange } ?: return
        if (!uiState.value.areActionsEnabled) return
        show(edit.start(comment.id, comment.body, composerOf(uiState.value)))
    }

    private fun stopEditing() {
        if (uiState.value.isEditingComment) show(edit.stop())
    }

    /*
     * Why: on the device first, offline too, as a new comment; the composer goes back to what was typed before, and
     * to the edit once more when it couldn't be kept, while nothing new was typed meanwhile.
     */
    private fun saveEdit(commentId: String) {
        val body = uiState.value.commentDraft
        val restored = edit.stop()
        show(restored)
        scope.launch {
            editBillComment(commentId, body).onFailure {
                if (composerOf(uiState.value) == restored) show(edit.start(commentId, body, restored))
                emit(BillDetailUiAction.ShowCommentChangeError)
            }
        }
    }

    private fun askToDelete(commentId: String) {
        if (findCommentOrNull(commentId)?.canChange != true || !uiState.value.areActionsEnabled) return
        uiState.update { it.copy(commentToDelete = commentId) }
    }

    // Why: a tombstone, offline too; an edit of it under way ends with it.
    private fun delete() {
        val commentId = uiState.value.commentToDelete ?: return
        uiState.update { it.copy(commentToDelete = null) }
        if (uiState.value.editingCommentId == commentId) stopEditing()
        scope.launch {
            deleteBillComment(commentId).onFailure { emit(BillDetailUiAction.ShowCommentChangeError) }
        }
    }

    // Why: the heart turns at once, since the device's copy changes first.
    private fun toggleLike(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        if (!uiState.value.areActionsEnabled) return
        val me = me()
        scope.launch {
            likeBillComment(
                commentId = comment.id,
                groupId = route.groupId,
                isLiked = !comment.isLikedByMe,
                memberId = me?.id,
                likerName = me?.name,
            ).onFailure { emit(BillDetailUiAction.ShowCommentChangeError) }
        }
    }

    private fun showLikes(commentId: String) {
        if ((findCommentOrNull(commentId)?.likeCount ?: 0) == 0) return
        uiState.update { it.copy(likesCommentId = commentId) }
    }

    // Why: the comment's link, which opens Conta at it for whoever is in the group, offline too.
    private fun share(commentId: String) {
        val comment = findCommentOrNull(commentId)?.takeIf { it.canInteract } ?: return
        val share = BillCommentShare(
            link = BillCommentLink.of(route.expenseId, comment.id),
            authorName = comment.authorName,
            isMe = comment.isMe,
            billDescription = uiState.value.description,
            body = comment.body,
        )
        scope.launch { emit(BillDetailUiAction.ShareComment(share)) }
    }
}

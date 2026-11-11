@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/*
 * Why: a bill's link page, blitzsplit.com/c/<code>, with the names Conta's state has for the same things, so the page
 * draws its comments and attachments with Conta's components.
 */
class JsSharedBillState(
    // Why: "loading", "open", "not-found", "members-only" or "failed".
    val status: String,
    val isOnline: Boolean,
    val canOpenInApp: Boolean,
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val description: String,
    val totalMinor: Double,
    val spentAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val spentDay: String,
    // Why: a bill without a time shows its date alone.
    val hasSpentTime: Boolean,
    val category: JsCategory,
    val categoryParent: JsCategory?,
    // Why: the SplitMode's code: "equal", "amount", "percent" or "shares".
    val splitMode: String,
    val participantCount: Int,
    val payers: Array<JsBillPayer>,
    val people: Array<JsBillShareRow>,
    // Why: the receipt first; empty hides "Anexos".
    val attachments: Array<JsBillAttachment>,
    val photoCount: Int,
    // Why: the viewer's page while it is open, else null.
    val viewedAttachmentIndex: Int?,
    val threads: Array<JsBillCommentThread>,
    val commentCount: Int,
    val commentDraft: String,
    val replyTarget: JsBillReplyTarget?,
    val showsNoComments: Boolean,
    val showsSignIn: Boolean,
    val showsSignInToComment: Boolean,
    val showsComposer: Boolean,
    val canSendComment: Boolean,
    // Why: "Editando comentário" over the composer, which holds the comment's text, while not null.
    val editingCommentId: String?,
    val isEditingComment: Boolean,
    // Why: "Apagar comentário?", open while not null.
    val commentToDelete: String?,
    // Why: "Curtidas", the comment whose likes are listed, open while not null.
    val likesComment: JsBillCommentRow?,
    // Why: the comment the link was shared at, scrolled to and highlighted once (focusedCommentShown), then null.
    val focusedCommentId: String?,
    // Why: the heart, "Editar" and "Apagar" go straight to the server: online, signed in and with none on its way.
    val canChangeComments: Boolean,
    // Why: an edit, a deletion or a like on its way, which "Apagar comentário?" waits for.
    val isChangingComment: Boolean,
)

/*
 * Why: type is "comment-error", "needs-name", "comment-change-error", "comment-gone" or "share-comment" (share); share
 * is null for anything but "share-comment".
 */
class JsSharedBillAction(
    val type: String,
    val share: JsBillCommentShare?,
)

class SharedBillPresenter(
    route: JsRoute,
) {
    private val sharedBillRoute = route.route as Route.SharedBill
    private val holder = ViewModelHolder(SharedBillViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(sharedBillRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsSharedBillState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsSharedBillState(
                status = toJsSharedBillStatus(state.status),
                isOnline = state.isOnline,
                canOpenInApp = state.canOpenInApp,
                groupName = state.groupName,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                description = state.description,
                totalMinor = state.totalMinor.toDouble(),
                spentAt = state.spentAt.toDouble(),
                spentDay = toJsRelativeDay(state.spentDay),
                hasSpentTime = state.hasSpentTime,
                category = toJsCategory(state.category),
                categoryParent = state.categoryParent?.let(::toJsCategory),
                splitMode = state.splitMode.code,
                participantCount = state.participantCount,
                payers = state.payers.map(::toJsBillPayer).toTypedArray(),
                people = state.people.map(::toJsBillShareRow).toTypedArray(),
                attachments = state.attachments.map(::toJsBillAttachment).toTypedArray(),
                photoCount = state.photoCount,
                viewedAttachmentIndex = state.viewedAttachmentIndex,
                threads = state.threads.map(::toJsBillCommentThread).toTypedArray(),
                commentCount = state.commentCount,
                commentDraft = state.commentDraft,
                replyTarget = state.replyTarget?.let(::toJsBillReplyTarget),
                showsNoComments = state.showsNoComments,
                showsSignIn = state.showsSignIn,
                showsSignInToComment = state.showsSignInToComment,
                showsComposer = state.showsComposer,
                canSendComment = state.canSendComment,
                editingCommentId = state.editingCommentId,
                isEditingComment = state.isEditingComment,
                commentToDelete = state.commentToDelete,
                likesComment = state.likesComment?.let(::toJsBillCommentRow),
                focusedCommentId = state.focusedCommentId,
                canChangeComments = state.canChangeComments,
                isChangingComment = state.isChangingComment,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsSharedBillAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                SharedBillUiAction.ShowCommentError -> JsSharedBillAction("comment-error", null)

                SharedBillUiAction.ShowNeedsName -> JsSharedBillAction("needs-name", null)

                SharedBillUiAction.ShowCommentChangeError -> JsSharedBillAction("comment-change-error", null)

                SharedBillUiAction.ShowCommentGone -> JsSharedBillAction("comment-gone", null)

                is SharedBillUiAction.ShareComment -> JsSharedBillAction(
                    type = "share-comment",
                    share = toJsBillCommentShare(action.share),
                )
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun retry() {
        viewModel.onEvent(SharedBillUiEvent.OnRetryClicked)
    }

    // Why: the browser's sign in leaves the page, so the link is kept for when it comes back (BlitzSplitWeb).
    fun signIn() {
        // Why: a link shared at a comment opens at it again once the browser comes back.
        val link = sharedBillRoute.commentId
            ?.let { commentId -> SharedBillLink.of(sharedBillRoute.code, commentId) }
            ?: SharedBillLink.of(sharedBillRoute.code)
        keepSharedBillForSignIn(link)
        viewModel.onEvent(SharedBillUiEvent.OnSignInClicked)
    }

    fun openInApp() {
        viewModel.onEvent(SharedBillUiEvent.OnOpenInAppClicked)
    }

    fun close() {
        viewModel.onEvent(SharedBillUiEvent.OnCloseClicked)
    }

    fun changeComment(text: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentChanged(text))
    }

    fun sendComment() {
        viewModel.onEvent(SharedBillUiEvent.OnSendCommentClicked)
    }

    fun reply(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnReplyClicked(commentId))
    }

    fun cancelReply() {
        viewModel.onEvent(SharedBillUiEvent.OnReplyCancelled)
    }

    fun editComment(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditClicked(commentId))
    }

    fun cancelEdit() {
        viewModel.onEvent(SharedBillUiEvent.OnCommentEditCancelled)
    }

    fun deleteComment(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteClicked(commentId))
    }

    fun confirmDeleteComment() {
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed)
    }

    fun dismissDeleteComment() {
        viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteDismissed)
    }

    fun likeComment(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikeClicked(commentId))
    }

    fun openLikes(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesClicked(commentId))
    }

    fun closeLikes() {
        viewModel.onEvent(SharedBillUiEvent.OnCommentLikesDismissed)
    }

    fun shareComment(commentId: String) {
        viewModel.onEvent(SharedBillUiEvent.OnCommentShareClicked(commentId))
    }

    fun focusedCommentShown() {
        viewModel.onEvent(SharedBillUiEvent.OnFocusedCommentShown)
    }

    fun openAttachment(id: String) {
        viewModel.onEvent(SharedBillUiEvent.OnAttachmentClicked(id))
    }

    fun showAttachment(id: String) {
        viewModel.onEvent(SharedBillUiEvent.OnViewerPageChanged(id))
    }

    fun closeViewer() {
        viewModel.onEvent(SharedBillUiEvent.OnViewerDismissed)
    }

    // Why: the viewer's "Baixar", which the page downloads itself.
    fun trackDownload() {
        viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsSharedBillStatus(status: SharedBillStatus): String = when (status) {
    SharedBillStatus.LOADING -> "loading"
    SharedBillStatus.OPEN -> "open"
    SharedBillStatus.NOT_FOUND -> "not-found"
    SharedBillStatus.MEMBERS_ONLY -> "members-only"
    SharedBillStatus.FAILED -> "failed"
}

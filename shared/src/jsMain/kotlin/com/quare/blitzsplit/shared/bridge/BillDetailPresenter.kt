@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform
import software.quare.loadable.valueOrNull

// Why: name null for "Ex-membro"; opensProfile, the face and name open their profile (openPerson).
class JsBillPayer(
    val memberId: String,
    val name: String?,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val opensProfile: Boolean,
)

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly; name null for "Ex-membro".
class JsBillShareRow(
    val memberId: String,
    val name: String?,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val hasJoined: Boolean,
    val paidMinor: Double?,
    val shareMinor: Double?,
    val opensProfile: Boolean,
)

/*
 * Why: the line under "Sua parte", flat. kind is "owes-payers" (names) or "owed-by" (names, eachMinor when they all
 * owe the same, and totalMinor set); a null name in names is "Ex-membro".
 */
class JsMyPartNote(
    val kind: String,
    val names: Array<String?>,
    val eachMinor: Double?,
    val totalMinor: Double?,
)

// Why: one comment or reply; authorName null when the server kept none ("Alguém"), replyToName the "@Diego" of a reply.
class JsBillCommentRow(
    val id: String,
    // Why: the author still in the group, whose face and name open their profile (openPerson); else null.
    val authorMemberId: String?,
    val authorName: String?,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val viaLink: Boolean,
    val replyToName: String?,
    val body: String,
    val postedAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the time.
    val postedDay: String,
    val isPending: Boolean,
)

class JsBillCommentThread(
    val comment: JsBillCommentRow,
    val replies: Array<JsBillCommentRow>,
)

// Why: "Respondendo a Diego"; name null for someone the server kept no name of.
class JsBillReplyTarget(
    val threadId: String,
    val commentId: String,
    val name: String?,
    val isMe: Boolean,
)

/*
 * Why: one of "Anexos": localImage (an Int8Array, the same one on every state, so the page makes one object URL per
 * id) for a photo added here, else imageUrl, the signed URL, null offline or until it is signed.
 */
class JsBillAttachment(
    val id: String,
    val isReceipt: Boolean,
    val localImage: ByteArray?,
    val imageUrl: String?,
    val width: Int?,
    val height: Int?,
    // Why: "Foto 2"; null for the receipt.
    val photoNumber: Int?,
    // Why: null for someone no longer in the group.
    val adderName: String?,
    val isAddedByMe: Boolean,
    val addedAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the time.
    val addedDay: String,
    val isPending: Boolean,
    val canRemove: Boolean,
)

class JsBillDetailState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val description: String,
    val totalMinor: Double,
    val spentAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val spentDay: String,
    // Why: this device's user first, then the group's order, then whoever left it.
    val payers: Array<JsBillPayer>,
    // Why: the lone payer whose line opens their profile; null for several payers, who open from their rows.
    val lonePayerId: String?,
    val isPending: Boolean,
    // Why: "Alimentação › Mercado" over the title.
    val category: JsCategory,
    val categoryParent: JsCategory?,
    val myBalanceMinor: Double?,
    val myPartNote: JsMyPartNote?,
    val showsMyPart: Boolean,
    // Why: the SplitMode's code: "equal", "amount", "percent" or "shares".
    val splitMode: String,
    val participantCount: Int,
    val people: Array<JsBillShareRow>,
    val deletion: JsBillDeletion?,
    val isDeleting: Boolean,
    val areActionsEnabled: Boolean,
    val isLoaded: Boolean,
    // Why: "Comentários", in the order they were written, each with its replies; null while they load (shimmer).
    val threads: Array<JsBillCommentThread>?,
    val commentDraft: String,
    val replyTarget: JsBillReplyTarget?,
    // Why: "Histórico", the most recent first; empty hides it, null while the server's history loads (shimmer).
    val history: Array<JsBillHistoryDay>?,
    // Why: the history shows only the device's lines: offline, or the server's read failed.
    val isHistoryFromDeviceOnly: Boolean,
    val isOnline: Boolean,
    val isUndoing: Boolean,
    // Why: null while the comments load.
    val commentCount: Int?,
    val canSendComment: Boolean,
    val showsNoComments: Boolean,
    // Why: "Anexos", the receipt first; null while they load (shimmer).
    val attachments: Array<JsBillAttachment>?,
    val photoCount: Int?,
    val photoLimit: Int,
    val showsAddPhoto: Boolean,
    val canAddPhoto: Boolean,
    val isAddingPhoto: Boolean,
    // Why: the viewer's page while it is open, else null.
    val viewedAttachmentIndex: Int?,
)

class BillDetailPresenter(
    route: JsRoute,
) {
    private val billRoute = route.route as Route.BillDetail
    private val holder = ViewModelHolder(BillDetailViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(billRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsBillDetailState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsBillDetailState(
                groupName = state.groupName,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                description = state.description,
                totalMinor = state.totalMinor.toDouble(),
                spentAt = state.spentAt.toDouble(),
                spentDay = toJsRelativeDay(state.spentDay),
                payers = state.payers.map(::toJsBillPayer).toTypedArray(),
                lonePayerId = state.lonePayer?.memberId,
                isPending = state.isPending,
                category = toJsCategory(state.category),
                categoryParent = state.categoryParent?.let(::toJsCategory),
                myBalanceMinor = state.myBalanceMinor?.toDouble(),
                myPartNote = state.myPartNote?.let(::toJsMyPartNote),
                showsMyPart = state.showsMyPart,
                splitMode = state.splitMode.code,
                participantCount = state.participantCount,
                people = state.people.map(::toJsBillShareRow).toTypedArray(),
                deletion = state.deletion?.let(::toJsBillDeletion),
                isDeleting = state.isDeleting,
                areActionsEnabled = state.areActionsEnabled,
                isLoaded = state.isLoaded,
                threads = state.threads
                    .valueOrNull()
                    ?.map(::toJsBillCommentThread)
                    ?.toTypedArray(),
                commentDraft = state.commentDraft,
                replyTarget = state.replyTarget?.let(::toJsBillReplyTarget),
                history = state.history
                    .valueOrNull()
                    ?.map { toJsBillHistoryDay(it, state.categories) }
                    ?.toTypedArray(),
                isHistoryFromDeviceOnly = state.isHistoryFromDeviceOnly,
                isOnline = state.isOnline,
                isUndoing = state.isUndoing,
                commentCount = state.commentCount,
                canSendComment = state.canSendComment,
                showsNoComments = state.showsNoComments,
                attachments = state.attachments
                    .valueOrNull()
                    ?.map(::toJsBillAttachment)
                    ?.toTypedArray(),
                photoCount = state.photoCount,
                photoLimit = state.photoLimit,
                showsAddPhoto = state.showsAddPhoto,
                canAddPhoto = state.canAddPhoto,
                isAddingPhoto = state.isAddingPhoto,
                viewedAttachmentIndex = state.viewedAttachmentIndex,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    /*
     * Why: "delete-error", "comment-error", "undo-error", "photo-unreadable", "photo-no-room", "photo-too-large",
     * "photo-error" or "remove-photo-error", a toast in the page's words.
     */
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                BillDetailUiAction.ShowDeleteError -> "delete-error"

                BillDetailUiAction.ShowCommentError -> "comment-error"

                BillDetailUiAction.ShowUndoError -> "undo-error"

                is BillDetailUiAction.ShowPhotoError -> when (action.error) {
                    BillPhotoError.UNREADABLE -> "photo-unreadable"
                    BillPhotoError.NO_ROOM -> "photo-no-room"
                    BillPhotoError.TOO_LARGE -> "photo-too-large"
                    BillPhotoError.FAILED -> "photo-error"
                }

                BillDetailUiAction.ShowRemovePhotoError -> "remove-photo-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(BillDetailUiEvent.OnBackClicked)
    }

    // Why: "Compartilhar", the bill's link over Conta.
    fun share() {
        viewModel.onEvent(BillDetailUiEvent.OnShareClicked)
    }

    fun edit() {
        viewModel.onEvent(BillDetailUiEvent.OnEditClicked)
    }

    fun openPerson(memberId: String) {
        viewModel.onEvent(BillDetailUiEvent.OnPersonClicked(memberId))
    }

    fun deleteBill() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteClicked)
    }

    fun confirmDelete() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        viewModel.onEvent(BillDetailUiEvent.OnDeleteDismissed)
    }

    fun changeComment(text: String) {
        viewModel.onEvent(BillDetailUiEvent.OnCommentChanged(text))
    }

    fun sendComment() {
        viewModel.onEvent(BillDetailUiEvent.OnSendCommentClicked)
    }

    fun reply(commentId: String) {
        viewModel.onEvent(BillDetailUiEvent.OnReplyClicked(commentId))
    }

    fun cancelReply() {
        viewModel.onEvent(BillDetailUiEvent.OnReplyCancelled)
    }

    fun undo() {
        viewModel.onEvent(BillDetailUiEvent.OnUndoClicked)
    }

    // Why: the file picked, as an Int8Array of its bytes.
    fun photoPicked(source: ByteArray) {
        viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(source))
    }

    fun photoUnreadable() {
        viewModel.onEvent(BillDetailUiEvent.OnPhotoUnreadable)
    }

    fun openAttachment(id: String) {
        viewModel.onEvent(BillDetailUiEvent.OnAttachmentClicked(id))
    }

    fun showAttachment(id: String) {
        viewModel.onEvent(BillDetailUiEvent.OnViewerPageChanged(id))
    }

    fun closeViewer() {
        viewModel.onEvent(BillDetailUiEvent.OnViewerDismissed)
    }

    fun removeAttachment(id: String) {
        viewModel.onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(id))
    }

    fun dispose() {
        holder.clear()
    }
}

// Why: shared with a bill's link page (SharedBillPresenter), which draws the bill as Conta does.
internal fun toJsBillPayer(payer: BillPayer): JsBillPayer = JsBillPayer(
    memberId = payer.memberId,
    name = payer.name,
    initials = payer.initials,
    color = payer.avatarColor.code,
    photoUrl = payer.photoUrl,
    isMe = payer.isMe,
    opensProfile = payer.opensProfile,
)

internal fun toJsBillShareRow(row: BillShareRow): JsBillShareRow = JsBillShareRow(
    memberId = row.memberId,
    name = row.name,
    initials = row.initials,
    color = row.avatarColor.code,
    photoUrl = row.photoUrl,
    isMe = row.isMe,
    hasJoined = row.hasJoined,
    paidMinor = row.paidMinor?.toDouble(),
    shareMinor = row.shareMinor?.toDouble(),
    opensProfile = row.opensProfile,
)

private fun toJsMyPartNote(note: MyPartNote): JsMyPartNote = when (note) {
    is MyPartNote.OwesPayers -> JsMyPartNote(
        kind = "owes-payers",
        names = note.names.toTypedArray(),
        eachMinor = null,
        totalMinor = null,
    )

    is MyPartNote.OwedBy -> JsMyPartNote(
        kind = "owed-by",
        names = note.names.toTypedArray(),
        eachMinor = note.eachMinor?.toDouble(),
        totalMinor = note.totalMinor.toDouble(),
    )
}

private fun toJsBillCommentRow(row: BillCommentRow): JsBillCommentRow = JsBillCommentRow(
    id = row.id,
    authorMemberId = row.authorMemberId,
    authorName = row.authorName,
    initials = row.initials,
    color = row.avatarColor.code,
    photoUrl = row.photoUrl,
    isMe = row.isMe,
    viaLink = row.viaLink,
    replyToName = row.replyToName,
    body = row.body,
    postedAt = row.postedAt.toDouble(),
    postedDay = toJsRelativeDay(row.postedDay),
    isPending = row.isPending,
)

internal fun toJsBillCommentThread(thread: BillCommentThread): JsBillCommentThread = JsBillCommentThread(
    comment = toJsBillCommentRow(thread.comment),
    replies = thread.replies.map(::toJsBillCommentRow).toTypedArray(),
)

internal fun toJsBillReplyTarget(target: BillReplyTarget): JsBillReplyTarget = JsBillReplyTarget(
    threadId = target.threadId,
    commentId = target.commentId,
    name = target.name,
    isMe = target.isMe,
)

internal fun toJsBillAttachment(tile: BillAttachmentTile): JsBillAttachment = JsBillAttachment(
    id = tile.id,
    isReceipt = tile.isReceipt,
    localImage = tile.localImage,
    imageUrl = tile.imageUrl,
    width = tile.width,
    height = tile.height,
    photoNumber = tile.photoNumber,
    adderName = tile.adderName,
    isAddedByMe = tile.isAddedByMe,
    addedAt = tile.addedAt.toDouble(),
    addedDay = toJsRelativeDay(tile.addedDay),
    isPending = tile.isPending,
    canRemove = tile.canRemove,
)

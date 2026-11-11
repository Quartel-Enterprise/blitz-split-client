package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.countShownComments
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.isUnchangedEdit

/*
 * Why: a bill's link opened (7.3h), blitzsplit.com/c/<code>: the bill, who paid, each one's part, the attachments and
 * the comments, never the balances nor the group's other bills. Anyone signed in comments, in the group or not.
 */
data class SharedBillUiState(
    val status: SharedBillStatus,
    val isOnline: Boolean,
    // Why: "Entrar" in the header and "Entrar para comentar" while not.
    val isSignedIn: Boolean,
    // Why: a Google or Apple account, the only one a link the group opens may be waiting for.
    val hasLinkedAccount: Boolean,
    // Why: someone in the group with the bill on this device, who opens it in the app ("Abrir no app").
    val canOpenInApp: Boolean,
    // Why: "Bruno compartilhou esta conta do grupo Viagem Floripa" names the group.
    val groupName: String,
    val currency: GroupCurrency,
    val description: String,
    val totalMinor: Long,
    val spentAt: Long,
    val spentDay: RelativeDay,
    val categoryId: String?,
    // Why: the app's categories only: a group's own is the group's, so it shows as Outros here.
    val categories: CategoryTree,
    val splitMode: SplitMode,
    val payers: List<BillPayer>,
    // Why: whoever paid or has a part, in the bill's order.
    val people: List<BillShareRow>,
    val attachments: List<BillAttachmentTile>,
    val viewedAttachmentId: String?,
    val threads: List<BillCommentThread>,
    val commentDraft: String,
    val replyTarget: BillReplyTarget?,
    val isSendingComment: Boolean,
    // Why: "Editando comentário" over the composer, which holds the comment's text, while not null.
    val editingCommentId: String?,
    // Why: "Apagar comentário?", open while not null.
    val commentToDelete: String?,
    // Why: "Curtidas", who liked the comment, open while not null.
    val likesCommentId: String?,
    // Why: the comment the link was shared at: scrolled to and highlighted once, then null.
    val focusedCommentId: String?,
    // Why: an edit, a deletion or a like on its way to the server, which the page reads again after.
    val isChangingComment: Boolean,
) {
    val category: Category
        get() = categories.resolve(categoryId)

    val categoryParent: Category?
        get() = category.parentId?.let { categories.findOrNull(it) }

    // Why: the count in "Igual entre 5".
    val participantCount: Int
        get() = people.count { it.shareMinor != null }

    val photoCount: Int
        get() = attachments.count { !it.isReceipt }

    val viewedAttachmentIndex: Int?
        get() = attachments.indexOfFirst { it.id == viewedAttachmentId }.takeIf { it >= 0 }

    val viewedAttachment: BillAttachmentTile?
        get() = viewedAttachmentIndex?.let(attachments::get)

    val commentCount: Int
        get() = threads.countShownComments()

    // Why: liking, editing and deleting go straight to the server, as commenting does here.
    val canChangeComments: Boolean
        get() = showsComposer && isOnline && !isChangingComment

    val likesComment: BillCommentRow?
        get() = likesCommentId?.let(threads::findComment)?.takeIf { it.likeCount > 0 }

    val isEditingComment: Boolean
        get() = editingCommentId != null

    val showsNoComments: Boolean
        get() = status == SharedBillStatus.OPEN && threads.isEmpty()

    // Why: "Quer comentar? Entre com uma conta. Não precisa estar no grupo." in the composer's place.
    val showsSignInToComment: Boolean
        get() = status == SharedBillStatus.OPEN && !isSignedIn

    // Why: "Entrar" in the header, also over a link only the group opens.
    val showsSignIn: Boolean
        get() = when (status) {
            SharedBillStatus.OPEN -> !isSignedIn

            // Why: only the group's own Google or Apple account opens it, so an anonymous one may sign in too.
            SharedBillStatus.MEMBERS_ONLY -> !hasLinkedAccount

            else -> false
        }

    // Why: the composer only for someone signed in; it goes straight to the server.
    val showsComposer: Boolean
        get() = status == SharedBillStatus.OPEN && isSignedIn

    val canSendComment: Boolean
        get() = showsComposer &&
            isOnline &&
            !isSendingComment &&
            BillCommentRules.isBodyValid(commentDraft) &&
            !threads.isUnchangedEdit(editingCommentId, commentDraft)
}

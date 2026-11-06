package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.countShownComments
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.findComment
import com.quare.blitzsplit.feature.billdetail.presentation.mapper.isUnchangedEdit
import software.quare.loadable.Loadable
import software.quare.loadable.valueOrNull

data class BillDetailUiState(
    // Why: the top bar's title over the bill, as Despesas do grupo is the screen under it.
    val groupName: String,
    val currency: GroupCurrency,
    val description: String,
    val totalMinor: Long,
    // Why: "ontem, 27 de out."; each UI writes the date and the time, after "Hoje" or "Ontem" when it is one of them.
    val spentAt: Long,
    val spentDay: RelativeDay,
    // Why: who paid, this device's user first, then in the order they were added to the group, then who left it.
    val payers: List<BillPayer>,
    // Why: the bill's category (7.7), resolved in the group's categories; null for none.
    val categoryId: String?,
    val categories: CategoryTree,
    // Why: "Na fila · sincroniza ao reconectar".
    val isPending: Boolean,
    // Why: "Sua parte", what the bill does to this device's user's balance; null when they aren't in it.
    val myBalanceMinor: Long?,
    val myPartNote: MyPartNote?,
    // Why: "Igual entre 5", "Por valor entre 6".
    val splitMode: SplitMode,
    // Why: everyone in the group in the order they were added, then whoever left it and is still in the bill.
    val people: List<BillShareRow>,
    // Why: "Apagar esta conta?", open while not null.
    val deletion: BillDeletion?,
    val isDeleting: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    /*
     * Why: "Comentários", in the order they were written, each with its replies. Loading until read and while a
     * restored account's copy is not whole, since the comments only come with its first full sync.
     */
    val threads: Loadable<List<BillCommentThread>>,
    // Why: what is typed in "Escreva um comentário".
    val commentDraft: String,
    // Why: "Respondendo a Diego" while not null.
    val replyTarget: BillReplyTarget?,
    // Why: "Editando comentário" over the composer, which holds the comment's text, while not null.
    val editingCommentId: String?,
    // Why: "Apagar comentário?", open while not null.
    val commentToDelete: String?,
    // Why: "Curtidas", who liked the comment, open while not null.
    val likesCommentId: String?,
    // Why: the comment a link opened Conta at: scrolled to and highlighted once, then null.
    val focusedCommentId: String?,
    /*
     * Why: "Histórico", the most recent first, by day; never the comments. Loading online until the server's history
     * answered; offline, or once that read failed, the device's lines (isHistoryFromDeviceOnly).
     */
    val history: Loadable<List<BillHistoryDay>>,
    // Why: the history holds only what the device keeps (its 90 days of Atividade): offline, or the server failed.
    val isHistoryFromDeviceOnly: Boolean,
    // Why: "Sem internet. Comentários e fotos ficam na fila e saem ao reconectar."
    val isOnline: Boolean,
    // Why: "Desfazer" saving the bill as it was.
    val isUndoing: Boolean,
    // Why: "Anexos", the receipt first, then the photos in the order they were added; Loading like the comments.
    val attachments: Loadable<List<BillAttachmentTile>>,
    // Why: the attachment the viewer shows, full screen, while not null.
    val viewedAttachmentId: String?,
    // Why: a photo picked and being made ready, which takes a moment on a large one.
    val isAddingPhoto: Boolean,
) {
    // Why: "Ana pagou" opens Ana's profile; several payers each have their row in the split, which opens them instead.
    val lonePayer: BillPayer?
        get() = payers.singleOrNull()?.takeIf { it.opensProfile }

    // Why: "Anexos · 2 de 5 fotos"; the receipt isn't one of them. Null while the attachments load.
    val photoCount: Int?
        get() = attachments.valueOrNull()?.count { !it.isReceipt }

    val photoLimit: Int
        get() = BillAttachmentKind.PHOTO.limit

    // Why: the "Adicionar foto" tile, there while the bill has room for one more photo.
    val showsAddPhoto: Boolean
        get() = photoCount?.let { it < photoLimit } == true

    // Why: "Adicionar foto" while the bill has room, offline too.
    val canAddPhoto: Boolean
        get() = areActionsEnabled && !isAddingPhoto && showsAddPhoto

    // Why: the viewer's page, gone with the attachment (removed on another device meanwhile).
    val viewedAttachmentIndex: Int?
        get() = attachments
            .valueOrNull()
            ?.indexOfFirst { it.id == viewedAttachmentId }
            ?.takeIf { it >= 0 }

    fun hasAttachment(id: String): Boolean = attachments.valueOrNull()?.any { it.id == id } == true

    val viewedAttachment: BillAttachmentTile?
        get() = viewedAttachmentIndex?.let { attachments.valueOrNull()?.getOrNull(it) }

    // Why: the count beside "Comentários", replies included and "Comentário apagado" not; null while they load.
    val commentCount: Int?
        get() = threads.valueOrNull()?.countShownComments()

    // Why: the sheet of who liked it closes once the comment or its last like is gone.
    val likesComment: BillCommentRow?
        get() = likesCommentId
            ?.let { threads.valueOrNull()?.findComment(it) }
            ?.takeIf { it.likeCount > 0 }

    val isEditingComment: Boolean
        get() = editingCommentId != null

    // Why: "Enviar comentário" stays disabled until something is typed, up to what the server takes.
    val canSendComment: Boolean
        get() = isLoaded &&
            !isDeleting &&
            BillCommentRules.isBodyValid(commentDraft) &&
            threads.valueOrNull()?.isUnchangedEdit(editingCommentId, commentDraft) != true

    // Why: "Nenhum comentário ainda. Pergunte ou combine algo sobre esta conta."
    val showsNoComments: Boolean
        get() = isLoaded && threads.valueOrNull()?.isEmpty() == true

    // Why: "Alimentação › Mercado" under the bill's title, "Outros" for none.
    val category: Category
        get() = categories.resolve(categoryId)

    val categoryParent: Category?
        get() = category.parentId?.let { categories.findOrNull(it) }

    // Why: the count in "Igual entre 5".
    val participantCount: Int
        get() = people.count { it.shareMinor != null }

    // Why: "Sua parte" only for someone in the bill.
    val showsMyPart: Boolean
        get() = isLoaded && myBalanceMinor != null

    // Why: Editar and Apagar do nothing while the bill is being deleted.
    val areActionsEnabled: Boolean
        get() = isLoaded && !isDeleting

    // Why: a bill in a currency this version doesn't know is read-only: Editar is off, with the note below.
    val canEdit: Boolean
        get() = areActionsEnabled && currency.isKnown

    // Why: "This bill uses a currency this app version doesn't know. Update the app to edit it."
    val showsUnknownCurrencyNote: Boolean
        get() = isLoaded && !currency.isKnown
}

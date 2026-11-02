package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

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
    // Why: "Comentários", in the order they were written, each with its replies.
    val threads: List<BillCommentThread>,
    // Why: what is typed in "Escreva um comentário".
    val commentDraft: String,
    // Why: "Respondendo a Diego" while not null.
    val replyTarget: BillReplyTarget?,
    // Why: "Histórico", the most recent first, by day; never the comments.
    val history: List<BillHistoryDay>,
    // Why: "Sem internet. Comentários e fotos ficam na fila e saem ao reconectar."
    val isOnline: Boolean,
    // Why: "Desfazer" saving the bill as it was.
    val isUndoing: Boolean,
    // Why: "Anexos", the receipt first, then the photos in the order they were added.
    val attachments: List<BillAttachmentTile>,
    // Why: the attachment the viewer shows, full screen, while not null.
    val viewedAttachmentId: String?,
    // Why: a photo picked and being made ready, which takes a moment on a large one.
    val isAddingPhoto: Boolean,
) {
    // Why: "Anexos · 2 de 5 fotos"; the receipt isn't one of them.
    val photoCount: Int
        get() = attachments.count { !it.isReceipt }

    val photoLimit: Int
        get() = BillAttachmentKind.PHOTO.limit

    // Why: the "Adicionar foto" tile, there while the bill has room for one more photo.
    val showsAddPhoto: Boolean
        get() = photoCount < photoLimit

    // Why: "Adicionar foto" while the bill has room, offline too.
    val canAddPhoto: Boolean
        get() = areActionsEnabled && !isAddingPhoto && showsAddPhoto

    // Why: the viewer's page, gone with the attachment (removed on another device meanwhile).
    val viewedAttachmentIndex: Int?
        get() = attachments.indexOfFirst { it.id == viewedAttachmentId }.takeIf { it >= 0 }

    val viewedAttachment: BillAttachmentTile?
        get() = viewedAttachmentIndex?.let(attachments::get)

    // Why: the count beside "Comentários", replies included.
    val commentCount: Int
        get() = threads.sumOf { 1 + it.replies.size }

    // Why: "Enviar comentário" stays disabled until something is typed, up to what the server takes.
    val canSendComment: Boolean
        get() = isLoaded && !isDeleting && BillCommentRules.isBodyValid(commentDraft)

    // Why: "Nenhum comentário ainda. Pergunte ou combine algo sobre esta conta."
    val showsNoComments: Boolean
        get() = isLoaded && threads.isEmpty()

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
}

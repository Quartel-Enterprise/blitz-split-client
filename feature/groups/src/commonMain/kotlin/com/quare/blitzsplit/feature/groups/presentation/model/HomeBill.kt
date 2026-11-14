package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: a row of Início's "Últimas despesas": "Mercado", the group's square and who paid as faces (beta feedback: no
 * "Viagem Floripa · Você pagou" line, as the legacy app drew them), "R$ 312,00 · +R$ 249,60". The names stay for the
 * screen readers and the web's table.
 */
data class HomeBill(
    val id: String,
    val groupId: String,
    val groupName: String,
    // Why: the group's rounded square, "VF" on its color, or its photo over them.
    val groupInitials: String,
    val groupColor: AvatarColor,
    val groupPhotoUrl: String?,
    val description: String,
    val totalMinor: Long,
    val currency: GroupCurrency,
    // Why: who paid, this device's user first, as Despesas names them.
    val payers: List<SearchBillPayer>,
    // Why: their faces in that order, as FaceStack shows them; the rest is "+N" (extraPayerCount). Someone gone has no initial.
    val payerFaces: List<MemberFace>,
    val extraPayerCount: Int,
    // Why: what the bill does to this device's user's balance: positive "a receber", negative "você deve".
    val myBalanceMinor: Long,
    // Why: the row's badge, as Despesas draws it (6.1): its category as it reads now, "Outros" for none.
    val category: Category,
    // Why: "na fila" and the amber dot: saved here and not on the server yet.
    val isPending: Boolean,
)

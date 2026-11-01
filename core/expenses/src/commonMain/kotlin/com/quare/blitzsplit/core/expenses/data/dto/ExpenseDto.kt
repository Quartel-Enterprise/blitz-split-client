package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    // Why: the server keeps whoever created the bill, whatever a later push sends.
    @SerialName("created_by") val createdBy: String?,
    @SerialName("description") val description: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("split_mode") val splitMode: String,
    @SerialName("payers") val payers: List<ExpensePayerDto>,
    @SerialName("shares") val shares: List<ExpenseShareDto>,
    // Why: "Itens da conta" the shares come from, sent as null so a split by other means clears them on the server.
    @Serializable(with = LenientExpenseItemsSerializer::class)
    @SerialName("items") val items: ExpenseItemsDto?,
    @SerialName("spent_at") val spentAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    // Why: who last saved the bill, which the server sets from the session whatever a push sends.
    @SerialName("updated_by") val updatedBy: String?,
)

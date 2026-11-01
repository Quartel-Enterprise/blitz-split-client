package com.quare.blitzsplit.core.expenses.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why: "Itens da conta" as the server keeps them in expenses.items and Room in its column. updated_at is the bill's
 * version they were saved with: a version that doesn't know items saves the bill without them, so items whose
 * updated_at isn't the bill's no longer give its shares, and both the server and this version drop them.
 */
@Serializable
data class ExpenseItemsDto(
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("items") val items: List<ExpenseItemDto>,
    @SerialName("service_percent") val servicePercent: Long,
    @SerialName("couvert_minor") val couvertMinor: Long,
    @SerialName("couvert_member_ids") val couvertMemberIds: List<String>,
    @SerialName("extra_minor") val extraMinor: Long,
    @SerialName("from_receipt") val fromReceipt: Boolean,
)

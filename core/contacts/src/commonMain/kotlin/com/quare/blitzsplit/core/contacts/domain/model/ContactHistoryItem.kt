package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Histórico com Carla", a bill or a payment between this device's user and the person. The bills' effects and
 * the payments add up to the net: +148,40 +32,50 −98,00, and a payment of 50,00 from her, make +32,90.
 */
sealed interface ContactHistoryItem {
    val id: String
    val groupName: String
    val currency: GroupCurrency

    // Why: "Quando", in epoch millis, the most recent first.
    val at: Long

    data class Bill(
        override val id: String,
        override val groupName: String,
        override val currency: GroupCurrency,
        override val at: Long,
        val description: String,
        // Why: "Carla pagou R$ 196,00" or "você pagou R$ 130,00": who paid the whole bill, null for several payers.
        val payerName: String?,
        val isPaidByMe: Boolean,
        val payerCount: Int,
        val totalMinor: Long,
        // Why: what the bill moves between the two: positive when it puts it on them, negative when on you.
        val effectMinor: Long,
    ) : ContactHistoryItem

    data class Payment(
        override val id: String,
        override val groupName: String,
        override val currency: GroupCurrency,
        override val at: Long,
        // Why: "Carla te pagou" (recebido) or "Você pagou Felipe" (pago).
        val isFromMe: Boolean,
        val amountMinor: Long,
        val method: SettlementMethod,
    ) : ContactHistoryItem
}

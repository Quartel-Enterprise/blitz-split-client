package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Várias pessoas pagaram", each one saying how much they paid of the bill: the one Rachar conta saves and the
 * one the sheet edits until "Pronto". A row left blank or at 0 paid nothing.
 */
data class PayerAmounts(
    // Why: the group's members in the order they were added, then whoever left it and paid part of a bill edited here.
    val rows: List<PayerAmountRow>,
    // Why: the bill's amount, 0 while the field holds no valid amount.
    val totalMinor: Long,
    val currency: GroupCurrency,
) {
    /*
     * Why: who paid and how much, in the rows' order. This and the facts below are worked out once per state, like
     * SplitEditor's shares, since every UI reads them many times on each keystroke.
     */
    val payers: List<ExpensePayer> = rows.mapNotNull { row ->
        AmountInput
            .parse(row.amountText, currency)
            ?.takeIf { it > 0 }
            ?.let {
                ExpensePayer(
                    memberId = row.memberId,
                    amountMinor = it,
                )
            }
    }

    // Why: "R$ 312,00 de R$ 312,00".
    val paidMinor: Long = payers.sumOf { it.amountMinor }

    // Why: "Faltam R$ 112,00" while positive, "Passou R$ 12,00" while negative.
    val remainingMinor: Long = totalMinor - paidMinor

    // Why: "Fechou": the amounts add up to the total, which "Pronto" and Salvar wait for.
    val addsUp: Boolean = totalMinor > 0 && remainingMinor == 0L

    // Why: what is missing or over, only while there is a total to compare with.
    val showsMismatch: Boolean = totalMinor > 0 && remainingMinor != 0L

    // Why: the rows of whoever paid something, as "Quem pagou" and Dividir name them ("Você e Ana", "3 pessoas").
    val paying: List<PayerAmountRow> = rows.associateBy { it.memberId }.let { rowsById ->
        val members = rows.filter { it.person != null }
        PayerOrder(
            meId = members.firstOrNull { it.person?.isMe == true }?.memberId,
            memberIds = members.map { it.memberId },
        ).sort(payers.map { it.memberId }).mapNotNull(rowsById::get)
    }
}

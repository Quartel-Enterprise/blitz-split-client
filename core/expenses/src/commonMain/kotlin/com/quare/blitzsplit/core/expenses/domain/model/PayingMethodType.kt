package com.quare.blitzsplit.core.expenses.domain.model

enum class PayingMethodType(
    // Why: the server's `type` column.
    val code: String,
    val isCard: Boolean = false,
    // Why: one wallet of cash is enough; cards, accounts and the rest may repeat.
    val isOnePerPerson: Boolean = false,
    // Why: "Other" means nothing without a name.
    val needsName: Boolean = false,
) {
    CREDIT_CARD("credit_card", isCard = true),
    DEBIT_CARD("debit_card", isCard = true),
    CASH("cash", isOnePerPerson = true),
    PIX("pix"),
    BANK_TRANSFER("bank_transfer"),
    BITCOIN("bitcoin"),
    OTHER("other", needsName = true),
    ;

    companion object {
        // Why: a type a newer app version added is skipped, not crashed on.
        fun ofCode(code: String): PayingMethodType? = entries.find { it.code == code }
    }
}

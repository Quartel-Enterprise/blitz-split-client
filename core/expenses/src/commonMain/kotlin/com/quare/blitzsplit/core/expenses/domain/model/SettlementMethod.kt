package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Como foi pago" of a payment recorded in Registrar pagamento; each UI writes its name.
enum class SettlementMethod(
    val code: String,
) {
    PIX("pix"),
    CASH("cash"),
    TRANSFER("transfer"),
    OTHER("other"),
    ;

    companion object {
        /*
         * Why: a method a later version adds still settles the same debt, so it reads as "Outro" rather than leaving
         * the balances.
         */
        fun fromCode(code: String): SettlementMethod = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

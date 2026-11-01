package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what Registrar pagamento just recorded, owed "Pagamento de Ana registrado." with Desfazer by the screen it
 * closed over.
 */
sealed interface RecordedPayment {
    // Why: undone together.
    val settlementIds: List<String>

    // Why: a pair of one group, which the group's screens and the home name as the group has them.
    data class InGroup(
        val settlement: Settlement,
    ) : RecordedPayment {
        override val settlementIds: List<String>
            get() = listOf(settlement.id)
    }

    // Why: settling up with someone across groups from the global Saldos, one payment per group.
    data class SettledUp(
        override val settlementIds: List<String>,
        // Why: as Registrar pagamento named them; null for someone no longer in the group.
        val personName: String?,
        // Why: "Acertar" (you paid them) rather than "Recebi" (they paid you).
        val isPaidByMe: Boolean,
    ) : RecordedPayment
}

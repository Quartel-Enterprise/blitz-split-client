package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Pagamento de Ana registrado." with Desfazer, on the screen Registrar pagamento closed over. Each UI writes
 * it: "Pagamento para Bruno registrado." when this device's user paid.
 */
data class PaymentNotice(
    // Why: what Desfazer undoes together: one payment for a pair, one per group for a settling up.
    val settlementIds: List<String>,
    // Why: null for someone no longer in the group.
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
) {
    // Why: what the UIs key the notice by, so a new one shows afresh.
    val settlementId: String
        get() = settlementIds.first()

    companion object {
        fun of(
            settlement: Settlement,
            meId: String?,
            nameOf: (memberId: String) -> String?,
        ): PaymentNotice = PaymentNotice(
            settlementIds = listOf(settlement.id),
            fromName = nameOf(settlement.fromMemberId),
            fromIsMe = settlement.fromMemberId == meId,
            toName = nameOf(settlement.toMemberId),
        )

        // Why: the other person is whom Registrar pagamento named, on the side the settling up went.
        fun of(settledUp: RecordedPayment.SettledUp): PaymentNotice = PaymentNotice(
            settlementIds = settledUp.settlementIds,
            fromName = settledUp.personName.takeUnless { settledUp.isPaidByMe },
            fromIsMe = settledUp.isPaidByMe,
            toName = settledUp.personName.takeIf { settledUp.isPaidByMe },
        )
    }
}

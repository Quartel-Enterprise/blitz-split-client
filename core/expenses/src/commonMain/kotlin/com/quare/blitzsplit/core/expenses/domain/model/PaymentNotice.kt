package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Pagamento de Ana registrado." with Desfazer, on the group's screen Registrar pagamento closed over. Each UI
 * writes it: "Pagamento para Bruno registrado." when this device's user paid.
 */
data class PaymentNotice(
    val settlementId: String,
    // Why: null for someone no longer in the group.
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
) {
    companion object {
        fun of(
            settlement: Settlement,
            meId: String?,
            nameOf: (memberId: String) -> String?,
        ): PaymentNotice = PaymentNotice(
            settlementId = settlement.id,
            fromName = nameOf(settlement.fromMemberId),
            fromIsMe = settlement.fromMemberId == meId,
            toName = nameOf(settlement.toMemberId),
        )
    }
}

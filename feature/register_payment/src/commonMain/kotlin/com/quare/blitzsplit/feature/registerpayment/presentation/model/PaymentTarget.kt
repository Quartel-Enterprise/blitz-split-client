package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: what the form records, as Room has it now: one pair of a group, or someone across groups.
internal data class PaymentTarget(
    val currency: GroupCurrency,
    // Why: null for someone no longer in the group ("Ex-membro").
    val from: PaymentPerson?,
    val to: PaymentPerson?,
    val debtMinor: Long,
    val legs: List<SettlementLeg>,
    // Why: "Por grupo" of a settling up across groups; empty for a pair of one group.
    val groupParts: List<PersonGroupBalance>,
)

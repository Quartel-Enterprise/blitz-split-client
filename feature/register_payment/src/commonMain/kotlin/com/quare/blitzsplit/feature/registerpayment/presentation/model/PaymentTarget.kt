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
    // Why: the account of whoever receives, whose keys "Pagar Ana" shows; null for a guest or someone gone.
    val toUserId: String?,
    val debtMinor: Long,
    // Why: "Apê 302 · você deve" under "Pagar Ana"; null across several groups, which say how many instead.
    val groupName: String?,
    val legs: List<SettlementLeg>,
    // Why: "Por grupo" of a settling up across groups; empty for a pair of one group.
    val groupParts: List<PersonGroupBalance>,
    val simplifiedNote: SimplifiedPairNote?,
    // Why: the group's "Simplificar dívidas" and whoever last changed it, by name; null across groups.
    val simplifiesDebts: Boolean?,
    val simplifyChangedByName: String?,
)

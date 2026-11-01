@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome

// Why: "Apagar esta conta?", for Conta and Editar conta; the amount in the currency's minor unit.
class JsBillDeletion(
    val description: String,
    val myBalanceMinor: Double?,
    val settledPair: JsSettledPair?,
)

/*
 * Why: "Elisa já acertou com Bruno depois desta conta. … com a mudança, Elisa volta a dever R$ 20,00 a Bruno." (7.4b)
 * The outcome is "owes-again", "owes", "owes-less" or "even"; a null name is someone who left the group.
 */
class JsSettledPair(
    val payerName: String?,
    val payerIsMe: Boolean,
    val receiverName: String?,
    val receiverIsMe: Boolean,
    val outcome: String,
    val debtorName: String?,
    val debtorIsMe: Boolean,
    val creditorName: String?,
    val creditorIsMe: Boolean,
    val amountMinor: Double,
)

internal fun toJsBillDeletion(deletion: BillDeletion): JsBillDeletion = JsBillDeletion(
    description = deletion.description,
    myBalanceMinor = deletion.myBalanceMinor?.toDouble(),
    settledPair = deletion.settledPair?.let(::toJsSettledPair),
)

internal fun toJsSettledPair(change: SettledPairChange): JsSettledPair = JsSettledPair(
    payerName = change.payerName,
    payerIsMe = change.payerIsMe,
    receiverName = change.receiverName,
    receiverIsMe = change.receiverIsMe,
    outcome = when (change.outcome) {
        SettledPairOutcome.OWES_AGAIN -> "owes-again"
        SettledPairOutcome.OWES -> "owes"
        SettledPairOutcome.OWES_LESS -> "owes-less"
        SettledPairOutcome.EVEN -> "even"
    },
    debtorName = change.debtorName,
    debtorIsMe = change.debtorIsMe,
    creditorName = change.creditorName,
    creditorIsMe = change.creditorIsMe,
    amountMinor = change.amountMinor.toDouble(),
)

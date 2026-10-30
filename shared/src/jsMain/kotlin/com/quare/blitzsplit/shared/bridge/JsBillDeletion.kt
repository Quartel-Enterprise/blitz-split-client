@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion

// Why: "Apagar esta conta?", for Conta and Editar conta; the amount in the currency's minor unit.
class JsBillDeletion(
    val description: String,
    val myBalanceMinor: Double?,
)

internal fun toJsBillDeletion(deletion: BillDeletion): JsBillDeletion = JsBillDeletion(
    description = deletion.description,
    myBalanceMinor = deletion.myBalanceMinor?.toDouble(),
)

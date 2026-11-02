@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow

// Why: a pair in Saldos, already netted; a null name (and face) is someone no longer in the group.
class JsDebtRow(
    val fromMemberId: String,
    val toMemberId: String,
    val fromName: String?,
    val fromFace: JsGroupFace?,
    val toName: String?,
    val toFace: JsGroupFace?,
    val amountMinor: Double,
    // Why: "you-owe", "owes-you" or "between-others".
    val kind: String,
)

internal fun toJsDebtRow(row: DebtRow): JsDebtRow = JsDebtRow(
    fromMemberId = row.fromMemberId,
    toMemberId = row.toMemberId,
    fromName = row.fromName,
    fromFace = row.fromFace?.let(::toJsGroupFace),
    toName = row.toName,
    toFace = row.toFace?.let(::toJsGroupFace),
    amountMinor = row.amountMinor.toDouble(),
    kind = when (row.kind) {
        DebtKind.YOU_OWE -> "you-owe"
        DebtKind.OWES_YOU -> "owes-you"
        DebtKind.BETWEEN_OTHERS -> "between-others"
    },
)

@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmountRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: person is null for someone who left the group, "Ex-membro".
class JsPayerAmountRow(
    val memberId: String,
    val person: JsBillPerson?,
    val amountText: String,
)

// Why: "Várias pessoas pagaram", amounts in the currency's minor unit.
class JsPayerAmounts(
    val rows: Array<JsPayerAmountRow>,
    val totalMinor: Double,
    val paidMinor: Double,
    val remainingMinor: Double,
    val addsUp: Boolean,
    val showsMismatch: Boolean,
    val paying: Array<JsPayerAmountRow>,
)

private fun toJsPayerAmountRow(row: PayerAmountRow): JsPayerAmountRow = JsPayerAmountRow(
    memberId = row.memberId,
    person = row.person?.let(::toJsBillPerson),
    amountText = row.amountText,
)

internal fun toJsPayerAmounts(amounts: PayerAmounts): JsPayerAmounts = JsPayerAmounts(
    rows = amounts.rows.map(::toJsPayerAmountRow).toTypedArray(),
    totalMinor = amounts.totalMinor.toDouble(),
    paidMinor = amounts.paidMinor.toDouble(),
    remainingMinor = amounts.remainingMinor.toDouble(),
    addsUp = amounts.addsUp,
    showsMismatch = amounts.showsMismatch,
    paying = amounts.paying.map(::toJsPayerAmountRow).toTypedArray(),
)

// Why: "Paid with" while it shows: this device's user's own methods, the default first, and the one picked (null for none).
class JsPaidWith(
    val methods: Array<JsPayingMethod>,
    val methodId: String?,
)

internal fun toJsPaidWithOrNull(state: SplitBillUiState): JsPaidWith? = if (state.showsPaidWith) {
    JsPaidWith(
        methods = state.paidWith.methods
            .map { it.toJsPayingMethod() }
            .toTypedArray(),
        methodId = state.paidWith.method?.id,
    )
} else {
    null
}

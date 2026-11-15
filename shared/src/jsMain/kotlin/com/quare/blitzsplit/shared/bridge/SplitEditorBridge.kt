@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow

// Why: Dividir's editor as the web draws it, apart from SplitBillPresenter so each file keeps one responsibility.
class JsSplitRow(
    val person: JsBillPerson,
    val isIncluded: Boolean,
    val valueText: String,
    val parts: Double,
    val shareMinor: Double?,
    // Why: By income's percentage the income gives, shown beside the share; null in any other mode.
    val incomePercent: Double?,
)

// Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly.
class JsSplitEditor(
    // Why: the SplitMode's code: "equal", "amount", "percent", "shares", "items" or "income".
    val mode: String,
    val rows: Array<JsSplitRow>,
    val totalMinor: Double,
    val addsUp: Boolean,
    val included: Array<JsBillPerson>,
    val excluded: Array<JsBillPerson>,
    val includedCount: Int,
    val isEveryoneIncluded: Boolean,
    val perPersonMinor: Double,
    val assignedMinor: Double,
    val remainingMinor: Double,
    val withoutValue: Array<JsBillPerson>,
    val percentSum: Double,
    val remainingPercent: Double,
    val totalParts: Double,
    val partValueMinor: Double,
    val statusTitle: JsSplitFact,
    val statusDetails: Array<JsSplitFact>,
)

private fun toJsSplitRow(
    row: SplitRow,
    incomePercent: Long?,
): JsSplitRow = JsSplitRow(
    person = toJsBillPerson(row.person),
    isIncluded = row.isIncluded,
    valueText = row.valueText,
    parts = row.parts.toDouble(),
    shareMinor = row.shareMinor?.toDouble(),
    incomePercent = incomePercent?.toDouble(),
)

internal fun toJsSplitEditor(editor: SplitEditor): JsSplitEditor = JsSplitEditor(
    mode = editor.mode.code,
    rows = editor.rows
        .map { row -> toJsSplitRow(row, editor.incomePercents[row.person.id]) }
        .toTypedArray(),
    totalMinor = editor.totalMinor.toDouble(),
    addsUp = editor.addsUp,
    included = editor.included.map(::toJsBillPerson).toTypedArray(),
    excluded = editor.excluded.map(::toJsBillPerson).toTypedArray(),
    includedCount = editor.includedCount,
    isEveryoneIncluded = editor.isEveryoneIncluded,
    perPersonMinor = editor.perPersonMinor.toDouble(),
    assignedMinor = editor.assignedMinor.toDouble(),
    remainingMinor = editor.remainingMinor.toDouble(),
    withoutValue = editor.withoutValue.map(::toJsBillPerson).toTypedArray(),
    percentSum = editor.percentSum.toDouble(),
    remainingPercent = editor.remainingPercent.toDouble(),
    totalParts = editor.totalParts.toDouble(),
    partValueMinor = editor.partValueMinor.toDouble(),
    statusTitle = toJsSplitFact(editor.statusTitle),
    statusDetails = editor.statusDetails.map(::toJsSplitFact).toTypedArray(),
)

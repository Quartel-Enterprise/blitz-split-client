@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact

/*
 * Why: a SplitFact, flat. kind is "each", "nobody", "nobody-checked", "nobody-has-parts", "among", "out",
 * "amount-left", "amount-over", "amount-adds-up", "parts-match-total", "amount-so-far", "without-value",
 * "percent-left", "percent-over", "percent-adds-up", "parts", "person-parts", "group-default", "items" or "loose-items",
 * and only that fact's fields are set:
 * amountMinor is also the assigned amount of "amount-so-far" and the part's value of "parts", count the people of
 * "among", the parts of "parts" and "person-parts" and the items of "items" and "loose-items".
 */
class JsSplitFact(
    val kind: String,
    val amountMinor: Double?,
    val totalMinor: Double?,
    val count: Double?,
    val percent: Double?,
    val people: Array<JsBillPerson>,
    val person: JsBillPerson?,
)

internal fun toJsSplitFact(fact: SplitFact): JsSplitFact {
    val kind = when (fact) {
        is SplitFact.Each -> "each"
        SplitFact.Nobody -> "nobody"
        is SplitFact.NobodyChecked -> "nobody-checked"
        is SplitFact.NobodyHasParts -> "nobody-has-parts"
        is SplitFact.Among -> "among"
        is SplitFact.Out -> "out"
        is SplitFact.AmountLeft -> "amount-left"
        is SplitFact.AmountOver -> "amount-over"
        is SplitFact.AmountAddsUp -> "amount-adds-up"
        SplitFact.PartsMatchTotal -> "parts-match-total"
        is SplitFact.AmountSoFar -> "amount-so-far"
        is SplitFact.WithoutValue -> "without-value"
        is SplitFact.PercentLeft -> "percent-left"
        is SplitFact.PercentOver -> "percent-over"
        SplitFact.PercentAddsUp -> "percent-adds-up"
        is SplitFact.Parts -> "parts"
        is SplitFact.PersonParts -> "person-parts"
        SplitFact.GroupDefault -> "group-default"
        SplitFact.NewGroupDefault -> "new-group-default"
        is SplitFact.Items -> "items"
        is SplitFact.LooseItems -> "loose-items"
    }
    val amountMinor = when (fact) {
        is SplitFact.Each -> fact.amountMinor
        is SplitFact.AmountLeft -> fact.amountMinor
        is SplitFact.AmountOver -> fact.amountMinor
        is SplitFact.AmountSoFar -> fact.assignedMinor
        is SplitFact.Parts -> fact.partMinor
        else -> null
    }
    val totalMinor = when (fact) {
        is SplitFact.NobodyChecked -> fact.totalMinor
        is SplitFact.NobodyHasParts -> fact.totalMinor
        is SplitFact.Among -> fact.totalMinor
        is SplitFact.AmountAddsUp -> fact.totalMinor
        is SplitFact.AmountSoFar -> fact.totalMinor
        else -> null
    }
    val count = when (fact) {
        is SplitFact.Among -> fact.count.toLong()
        is SplitFact.Parts -> fact.count
        is SplitFact.PersonParts -> fact.parts
        is SplitFact.Items -> fact.count.toLong()
        is SplitFact.LooseItems -> fact.count.toLong()
        else -> null
    }
    val percent = when (fact) {
        is SplitFact.PercentLeft -> fact.percent
        is SplitFact.PercentOver -> fact.percent
        else -> null
    }
    val people = when (fact) {
        is SplitFact.Out -> fact.people
        is SplitFact.WithoutValue -> fact.people
        else -> emptyList()
    }
    return JsSplitFact(
        kind = kind,
        amountMinor = amountMinor?.toDouble(),
        totalMinor = totalMinor?.toDouble(),
        count = count?.toDouble(),
        percent = percent?.toDouble(),
        people = people.map(::toJsBillPerson).toTypedArray(),
        person = (fact as? SplitFact.PersonParts)?.person?.let(::toJsBillPerson),
    )
}

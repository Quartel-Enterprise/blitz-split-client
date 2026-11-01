package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact

private const val FULL_PERCENT = 100L

// Why: the words of what the shared split says about itself, in Dividir's status and under "Divisão".
@Composable
internal fun factTextOf(
    fact: SplitFact,
    money: MoneyFormatter,
): String = when (fact) {
    is SplitFact.Each -> stringResource(R.string.split_bill_each, money.toText(fact.amountMinor))

    SplitFact.Nobody -> stringResource(R.string.split_bill_status_nobody)

    is SplitFact.NobodyChecked -> stringResource(R.string.split_bill_status_nobody_equal, money.toText(fact.totalMinor))

    is SplitFact.NobodyHasParts -> stringResource(
        R.string.split_bill_status_nobody_shares,
        money.toText(fact.totalMinor),
    )

    is SplitFact.Among -> pluralStringResource(
        R.plurals.split_bill_status_among,
        fact.count,
        money.toText(fact.totalMinor),
        fact.count,
    )

    is SplitFact.Out -> excludedTextOf(fact.people)

    is SplitFact.AmountLeft -> stringResource(R.string.split_bill_status_amount_left, money.toText(fact.amountMinor))

    is SplitFact.AmountOver -> stringResource(R.string.split_bill_status_amount_over, money.toText(fact.amountMinor))

    is SplitFact.AmountAddsUp -> stringResource(
        R.string.split_bill_status_amount_closed,
        money.toText(fact.totalMinor),
    )

    SplitFact.PartsMatchTotal -> stringResource(R.string.split_bill_status_amount_closed_message)

    is SplitFact.AmountSoFar -> stringResource(
        R.string.split_bill_status_amount_progress,
        money.toText(fact.assignedMinor),
        money.toText(fact.totalMinor),
    )

    is SplitFact.WithoutValue -> withoutValueTextOf(fact.people)

    is SplitFact.PercentLeft -> stringResource(R.string.split_bill_status_percent_left, fact.percent)

    is SplitFact.PercentOver -> stringResource(R.string.split_bill_status_percent_over, fact.percent)

    SplitFact.PercentAddsUp -> stringResource(R.string.split_bill_status_percent_closed, FULL_PERCENT)

    is SplitFact.Parts -> pluralStringResource(
        R.plurals.split_bill_status_parts,
        fact.count.toInt(),
        fact.count,
        money.toText(fact.partMinor),
    )

    SplitFact.GroupDefault -> stringResource(R.string.split_bill_group_default)

    is SplitFact.PersonParts -> if (fact.person.isMe) {
        pluralStringResource(R.plurals.split_bill_status_my_parts, fact.parts.toInt(), fact.parts)
    } else {
        pluralStringResource(
            R.plurals.split_bill_status_person_parts,
            fact.parts.toInt(),
            fact.person.name,
            fact.parts,
        )
    }
}

// Why: the status card's first line, where Igual says "R$ 62,40 para cada um" rather than the row's shorter "cada".
@Composable
internal fun statusTitleTextOf(
    fact: SplitFact,
    money: MoneyFormatter,
): String = if (fact is SplitFact.Each) {
    stringResource(R.string.split_bill_status_each, money.toText(fact.amountMinor))
} else {
    factTextOf(
        fact = fact,
        money = money,
    )
}

// Why: a sentence under the status's first line; "Diego não participa" is also a row's note, so it takes the period here.
@Composable
internal fun statusDetailTextOf(
    fact: SplitFact,
    money: MoneyFormatter,
): String {
    val text = factTextOf(
        fact = fact,
        money = money,
    )
    return if (fact is SplitFact.Out) sentenceOf(text) else text
}

// Why: "Diego não participa", "Diego e Elisa não participam"; "Você" alone takes its own verb in English.
@Composable
private fun excludedTextOf(people: List<BillPerson>): String = if (people.size == 1 && people.first().isMe) {
    stringResource(R.string.split_bill_excluded_me)
} else {
    pluralStringResource(R.plurals.split_bill_excluded, people.size, namesOf(people))
}

// Why: "Elisa está sem valor.", only while the amounts don't add up.
@Composable
private fun withoutValueTextOf(people: List<BillPerson>): String = if (people.size == 1 && people.first().isMe) {
    stringResource(R.string.split_bill_status_without_value_me)
} else {
    pluralStringResource(R.plurals.split_bill_status_without_value, people.size, namesOf(people))
}

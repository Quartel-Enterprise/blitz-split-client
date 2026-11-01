package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
import kotlin.math.abs

private const val PART_SEPARATOR = " · "

@Composable
internal fun personNameOf(person: BillPerson): String =
    if (person.isMe) stringResource(R.string.split_bill_you) else person.name

@Composable
internal fun namesOf(people: List<BillPerson>): String = nameListOf(people.map { personNameOf(it) })

// Why: "Você e Ana" for two, "3 pessoas" for more: whoever paid several parts, "Ex-membro" for who left the group.
@Composable
internal fun payerNamesOf(amounts: PayerAmounts): List<String> = amounts.paying.map { row ->
    row.person?.let { personNameOf(it) } ?: stringResource(R.string.split_bill_former_member)
}

// Why: "Faltam R$ 112,00" or "Passou R$ 12,00": how far what several people paid is from the total.
@Composable
internal fun payersMismatchOf(
    amounts: PayerAmounts,
    money: MoneyFormatter,
): String {
    val amount = money.toText(abs(amounts.remainingMinor))
    return if (amounts.remainingMinor > 0) {
        stringResource(R.string.split_bill_status_amount_left, amount)
    } else {
        stringResource(R.string.split_bill_status_amount_over, amount)
    }
}

@Composable
internal fun sentenceOf(text: String): String = stringResource(R.string.split_bill_sentence, text)

// Why: "Igual entre 5", the title of Rachar conta's "Divisão".
@Composable
internal fun splitTitleOf(split: SplitEditor): String {
    val plural = when (split.mode) {
        SplitMode.EQUAL -> R.plurals.split_bill_mode_equal_among
        SplitMode.AMOUNT -> R.plurals.split_bill_mode_amount_among
        SplitMode.PERCENT -> R.plurals.split_bill_mode_percent_among
        SplitMode.SHARES -> R.plurals.split_bill_mode_shares_among
    }
    return pluralStringResource(plural, split.includedCount, split.includedCount)
}

/*
 * Why: "R$ 62,40 cada · Diego não participa" under "Divisão". A split that doesn't add up says what is missing, as
 * Dividir's status does, so the disabled Salvar has a reason in sight.
 */
@Composable
internal fun splitSubtitleOrNull(
    summary: List<SplitFact>,
    money: MoneyFormatter,
): String? = summary
    .map { fact ->
        factTextOf(
            fact = fact,
            money = money,
        )
    }.takeIf { it.isNotEmpty() }
    ?.joinToString(PART_SEPARATOR)

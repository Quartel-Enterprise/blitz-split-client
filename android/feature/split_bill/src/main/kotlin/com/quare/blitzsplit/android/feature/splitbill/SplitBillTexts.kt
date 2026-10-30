package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

private const val PART_SEPARATOR = " · "

@Composable
internal fun personNameOf(person: BillPerson): String =
    if (person.isMe) stringResource(R.string.split_bill_you) else person.name

@Composable
internal fun namesOf(people: List<BillPerson>): String = nameListOf(people.map { personNameOf(it) })

@Composable
internal fun sentenceOf(text: String): String = stringResource(R.string.split_bill_sentence, text)

// Why: "Hoje, 28 de out. · 21:30", "Ontem, 27 de out. · 21:30" or "Sex., 24 de out. · 21:30".
@Composable
internal fun spentAtTextOf(
    spentAt: Long,
    spentDay: RelativeDay,
    dates: DateTimeTextFormatter,
): String {
    val time = dates.toTimeText(spentAt)
    return when (spentDay) {
        RelativeDay.TODAY -> stringResource(R.string.split_bill_when_today, dates.toDateText(spentAt), time)
        RelativeDay.YESTERDAY -> stringResource(R.string.split_bill_when_yesterday, dates.toDateText(spentAt), time)
        RelativeDay.OTHER -> stringResource(R.string.split_bill_when_other, dates.toWeekdayDateText(spentAt), time)
    }
}

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
    split: SplitEditor,
    money: MoneyFormatter,
): String? = split.summary
    .map { fact ->
        factTextOf(
            fact = fact,
            money = money,
        )
    }.takeIf { it.isNotEmpty() }
    ?.joinToString(PART_SEPARATOR)

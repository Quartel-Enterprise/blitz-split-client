package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.format.paidBySeveralOf
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote

// Why: "Hoje, 21:30", "Ontem, 21:30" or "Sex., 24 de out., 21:30".
@Composable
internal fun spentAtTextOf(
    spentAt: Long,
    spentDay: RelativeDay,
    dates: DateTimeTextFormatter,
): String {
    val time = dates.toTimeText(spentAt)
    return when (spentDay) {
        RelativeDay.TODAY -> stringResource(R.string.bill_detail_when_today, time)
        RelativeDay.YESTERDAY -> stringResource(R.string.bill_detail_when_yesterday, time)
        RelativeDay.OTHER -> stringResource(R.string.bill_detail_when_other, dates.toWeekdayDateText(spentAt), time)
    }
}

@Composable
internal fun payerTextOf(payers: List<BillPayer>): String {
    if (payers.size > 1) return paidBySeveralOf(payers.map { payerNameOf(it) })
    val payer = payers.firstOrNull()
    val name = payer?.name
    return when {
        payer?.isMe == true -> stringResource(R.string.bill_detail_you_paid)
        name == null -> stringResource(R.string.bill_detail_former_member_paid)
        else -> stringResource(R.string.bill_detail_someone_paid, name)
    }
}

@Composable
private fun payerNameOf(payer: BillPayer): String = if (payer.isMe) {
    stringResource(R.string.bill_detail_you)
} else {
    payer.name ?: stringResource(R.string.bill_detail_former_member)
}

@Composable
internal fun rowNameOf(row: BillShareRow): String = if (row.isMe) {
    stringResource(R.string.bill_detail_you)
} else {
    row.name ?: stringResource(R.string.bill_detail_former_member)
}

// Why: "Pagou R$ 642,00", "Ainda não entrou" or "Não participa"; null for someone in the split with nothing to add.
@Composable
internal fun rowSubtitleOf(
    row: BillShareRow,
    money: MoneyFormatter,
): String? = when {
    row.paidMinor != null -> stringResource(R.string.bill_detail_paid, money.toText(row.paidMinor ?: 0L))
    !row.hasJoined -> stringResource(R.string.bill_detail_not_joined)
    row.shareMinor == null -> stringResource(R.string.bill_detail_not_in)
    else -> null
}

// Why: "Igual entre 5", over the people in the split.
@Composable
internal fun splitTitleOf(
    mode: SplitMode,
    count: Int,
): String {
    val plural = when (mode) {
        SplitMode.EQUAL -> R.plurals.bill_detail_mode_equal
        SplitMode.AMOUNT -> R.plurals.bill_detail_mode_amount
        SplitMode.PERCENT -> R.plurals.bill_detail_mode_percent
        SplitMode.SHARES -> R.plurals.bill_detail_mode_shares
    }
    return pluralStringResource(plural, count, count)
}

@Composable
internal fun myPartNoteTextOf(
    note: MyPartNote,
    money: MoneyFormatter,
): String = when (note) {
    is MyPartNote.OwesPayers -> {
        val formerMember = stringResource(R.string.bill_detail_former_member)
        stringResource(R.string.bill_detail_owes_payer, nameListOf(note.names.map { it ?: formerMember }))
    }

    is MyPartNote.OwedBy -> {
        val formerMember = stringResource(R.string.bill_detail_former_member)
        val names = nameListOf(note.names.map { it ?: formerMember })
        val each = note.eachMinor
        if (each != null) {
            pluralStringResource(R.plurals.bill_detail_owe_you_each, note.names.size, names, money.toText(each))
        } else {
            pluralStringResource(
                R.plurals.bill_detail_owe_you_total,
                note.names.size,
                names,
                money.toText(note.totalMinor),
            )
        }
    }
}

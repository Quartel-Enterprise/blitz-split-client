package com.quare.blitzsplit.android.feature.billdetail

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryPart

// Why: one "Bar → Restaurante" line under a history entry; the field names a part's person or a later change.
internal data class HistoryDiff(
    val field: String?,
    val before: String,
    val after: String,
)

// Why: "Você" for this device's user, "Alguém" when the server kept no name.
@Composable
internal fun personNameOf(
    name: String?,
    isMe: Boolean,
): String = when {
    isMe -> stringResource(R.string.bill_detail_you)
    name == null -> stringResource(R.string.bill_detail_someone)
    else -> name
}

// Why: "hoje, 22:40", "ontem, 22:40" or "27 de out., 22:40".
@Composable
internal fun commentTimeTextOf(
    row: BillCommentRow,
    dates: DateTimeTextFormatter,
): String {
    val time = dates.toTimeText(row.postedAt)
    return when (row.postedDay) {
        RelativeDay.TODAY -> stringResource(R.string.bill_detail_comment_today, time)
        RelativeDay.YESTERDAY -> stringResource(R.string.bill_detail_comment_yesterday, time)
        RelativeDay.OTHER -> stringResource(R.string.bill_detail_comment_other, dates.toDateText(row.postedAt), time)
    }
}

// Why: "Hoje", "Ontem" or "Sex., 24 de out." over each day of the history.
@Composable
internal fun historyDayTitleOf(
    day: BillHistoryDay,
    dates: DateTimeTextFormatter,
): String = when (day.relativeDay) {
    RelativeDay.TODAY -> stringResource(R.string.bill_detail_today)
    RelativeDay.YESTERDAY -> stringResource(R.string.bill_detail_yesterday)
    RelativeDay.OTHER -> dates.toWeekdayDateText(day.dayStartMillis)
}

// Why: what follows the name, "trocou a categoria": an edit says its first change, and the rest go under it.
@Composable
internal fun historyVerbOf(entry: BillHistoryEntry): String = when (entry.kind) {
    BillHistoryKind.CREATED -> stringResource(
        if (entry.isPending) R.string.bill_detail_history_created_offline else R.string.bill_detail_history_created,
    )

    BillHistoryKind.EDITED -> stringResource(
        when (entry.changes.firstOrNull()) {
            is BillHistoryChange.Category -> R.string.bill_detail_history_category

            is BillHistoryChange.Amount -> R.string.bill_detail_history_amount

            is BillHistoryChange.Currency -> R.string.bill_detail_history_currency

            is BillHistoryChange.Description -> R.string.bill_detail_history_description

            is BillHistoryChange.SpentAt -> R.string.bill_detail_history_spent_at

            is BillHistoryChange.Payers -> R.string.bill_detail_history_payers

            is BillHistoryChange.Split -> R.string.bill_detail_history_split

            // Why: an edit still in the queue has no changes yet, which only the server writes.
            null -> if (entry.isPending) {
                R.string.bill_detail_history_edited_offline
            } else {
                R.string.bill_detail_history_edited
            }
        },
    )

    BillHistoryKind.ATTACHMENTS_ADDED -> if (entry.isReceipt) {
        stringResource(R.string.bill_detail_history_receipt_added)
    } else {
        pluralStringResource(
            R.plurals.bill_detail_history_photos_added,
            entry.attachmentCount,
            entry.attachmentCount,
        )
    }

    BillHistoryKind.ATTACHMENT_REMOVED -> stringResource(
        if (entry.isReceipt) {
            R.string.bill_detail_history_receipt_removed
        } else {
            R.string.bill_detail_history_photo_removed
        },
    )

    BillHistoryKind.LINK_CHANGED -> stringResource(
        when (entry.linkVisibility) {
            BillLinkVisibility.ANYONE -> R.string.bill_detail_history_link_anyone
            BillLinkVisibility.MEMBERS -> R.string.bill_detail_history_link_members
            null -> R.string.bill_detail_history_link_off
        },
    )
}

// Why: the bill as it was created, "Igual entre 5 · R$ 312,00", skipping what the event doesn't say.
@Composable
internal fun historyNoteOf(
    entry: BillHistoryEntry,
    money: MoneyFormatter,
): String? {
    if (entry.kind != BillHistoryKind.CREATED) return null
    val split = entry.splitMode?.let { mode ->
        entry.sharesCount?.let { count -> splitTitleOf(mode, count) } ?: splitModeLabelOf(mode)
    }
    val amount = entry.amountMinor?.let(money::toText)
    return if (split != null && amount != null) {
        stringResource(R.string.bill_detail_change_note, split, amount)
    } else {
        split ?: amount
    }
}

/*
 * Why: the lines under an edit: its first change without a field (the verb says it), each later one with its field,
 * and a split's parts each under the name of whoever's part moved.
 */
@Composable
internal fun historyDiffsOf(
    entry: BillHistoryEntry,
    categories: CategoryTree,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
): List<HistoryDiff> = entry.changes.flatMapIndexed { index, change ->
    diffsOf(
        change = change,
        showsField = index > 0,
        categories = categories,
        money = money,
        dates = dates,
    )
}

@Composable
private fun diffsOf(
    change: BillHistoryChange,
    showsField: Boolean,
    categories: CategoryTree,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
): List<HistoryDiff> {
    val none = stringResource(R.string.bill_detail_change_none)
    return when (change) {
        is BillHistoryChange.Amount -> listOf(
            HistoryDiff(
                field = fieldOrNull(R.string.bill_detail_field_amount, showsField),
                before = change.beforeMinor?.let(money::toText) ?: none,
                after = change.afterMinor?.let(money::toText) ?: none,
            ),
        )

        is BillHistoryChange.Description -> listOf(
            HistoryDiff(
                field = fieldOrNull(R.string.bill_detail_field_description, showsField),
                before = change.before?.let { stringResource(R.string.bill_detail_change_quoted, it) } ?: none,
                after = change.after?.let { stringResource(R.string.bill_detail_change_quoted, it) } ?: none,
            ),
        )

        is BillHistoryChange.SpentAt -> listOf(
            HistoryDiff(
                field = fieldOrNull(R.string.bill_detail_field_spent_at, showsField),
                before = change.beforeMillis?.let { momentTextOf(it, dates) } ?: none,
                after = change.afterMillis?.let { momentTextOf(it, dates) } ?: none,
            ),
        )

        // Why: the codes the bill's currency is known by, "BRL → USD".
        is BillHistoryChange.Currency -> listOf(
            HistoryDiff(
                field = fieldOrNull(R.string.bill_detail_field_currency, showsField),
                before = change.before?.code ?: none,
                after = change.after?.code ?: none,
            ),
        )

        // Why: no category reads as Outros, and a deleted one as its parent, as the bill does.
        is BillHistoryChange.Category -> listOf(
            HistoryDiff(
                field = fieldOrNull(R.string.bill_detail_field_category, showsField),
                before = categoryName(categories.resolve(change.beforeId)),
                after = categoryName(categories.resolve(change.afterId)),
            ),
        )

        // Why: the same payers paying other amounts say each amount that moved, as the split's parts do.
        is BillHistoryChange.Payers -> if (change.parts.isEmpty()) {
            listOf(
                HistoryDiff(
                    field = fieldOrNull(R.string.bill_detail_field_payers, showsField),
                    before = payerNamesOf(change.before) ?: none,
                    after = payerNamesOf(change.after) ?: none,
                ),
            )
        } else {
            change.parts.map { part -> partDiffOf(part, money) }
        }

        is BillHistoryChange.Split -> splitDiffsOf(
            change = change,
            showsField = showsField,
            money = money,
        )
    }
}

// Why: "Igual → Partes" when the mode changed, then "Carla R$ 112,00 → R$ 98,00" for each part that moved.
@Composable
private fun splitDiffsOf(
    change: BillHistoryChange.Split,
    showsField: Boolean,
    money: MoneyFormatter,
): List<HistoryDiff> {
    val none = stringResource(R.string.bill_detail_change_none)
    val mode = if (change.beforeMode != change.afterMode) {
        HistoryDiff(
            field = fieldOrNull(R.string.bill_detail_field_split, showsField),
            before = change.beforeMode?.let { splitModeLabelOf(it) } ?: none,
            after = change.afterMode?.let { splitModeLabelOf(it) } ?: none,
        )
    } else {
        null
    }
    return listOfNotNull(mode) + change.parts.map { part -> partDiffOf(part, money) }
}

@Composable
private fun partDiffOf(
    part: BillHistoryPart,
    money: MoneyFormatter,
): HistoryDiff {
    val out = stringResource(R.string.bill_detail_change_out)
    return HistoryDiff(
        field = if (part.isMe) {
            stringResource(R.string.bill_detail_you)
        } else {
            part.name ?: stringResource(R.string.bill_detail_former_member)
        },
        before = part.beforeMinor?.let(money::toText) ?: out,
        after = part.afterMinor?.let(money::toText) ?: out,
    )
}

@Composable
private fun fieldOrNull(
    @StringRes field: Int,
    showsField: Boolean,
): String? = if (showsField) stringResource(field) else null

@Composable
private fun momentTextOf(
    millis: Long,
    dates: DateTimeTextFormatter,
): String = stringResource(R.string.bill_detail_when_other, dates.toDateText(millis), dates.toTimeText(millis))

// Why: "Ana e Bruno"; someone no longer in the group is "Ex-membro".
@Composable
private fun payerNamesOf(names: List<String?>): String? {
    if (names.isEmpty()) return null
    val formerMember = stringResource(R.string.bill_detail_former_member)
    return nameListOf(names.map { it ?: formerMember })
}

// Why: Dividir's own names for its modes, "Igual", "Valores", "%", "Partes", "Itens".
@Composable
internal fun splitModeLabelOf(mode: SplitMode): String = stringResource(
    when (mode) {
        SplitMode.EQUAL -> R.string.bill_detail_split_equal

        SplitMode.AMOUNT -> R.string.bill_detail_split_amount

        // Why: a bill split by income is saved by its percentages.
        SplitMode.PERCENT, SplitMode.INCOME -> R.string.bill_detail_split_percent

        SplitMode.SHARES -> R.string.bill_detail_split_shares

        SplitMode.ITEMS -> R.string.bill_detail_split_items
    },
)

package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsItemRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsPersonRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus

private const val SENTENCE_SEPARATOR = " "
private const val FULL_PERCENT = 100L

// Why: the first line of "Itens da conta"'s status: "2 itens sem ninguém", "Faltam R$ 15,00" or "Fechou: R$ 642,00".
@Composable
internal fun itemsStatusTitleOf(
    status: ItemsStatus,
    money: MoneyFormatter,
): String = when (status) {
    is ItemsStatus.Loose -> pluralStringResource(R.plurals.split_bill_items_loose, status.items.size, status.items.size)

    is ItemsStatus.AmountLeft -> stringResource(
        R.string.split_bill_status_amount_left,
        money.toText(status.amountMinor),
    )

    is ItemsStatus.AmountOver -> stringResource(
        R.string.split_bill_status_amount_over,
        money.toText(status.amountMinor),
    )

    is ItemsStatus.AddsUp -> stringResource(R.string.split_bill_status_amount_closed, money.toText(status.totalMinor))
}

@Composable
internal fun itemsStatusDetailOf(
    status: ItemsStatus,
    money: MoneyFormatter,
): String = when (status) {
    is ItemsStatus.Loose -> if (status.isEveryItem) {
        stringResource(R.string.split_bill_items_loose_all)
    } else {
        pluralStringResource(
            R.plurals.split_bill_items_loose_some,
            status.items.size,
            nameListOf(status.items.map { it.name }),
            money.toText(status.looseMinor),
        )
    }

    is ItemsStatus.AmountLeft -> sumTextOf(status.sumMinor, status.totalMinor, status.isFromReceipt, money)

    is ItemsStatus.AmountOver -> sumTextOf(status.sumMinor, status.totalMinor, status.isFromReceipt, money)

    is ItemsStatus.AddsUp -> if (status.extraMinor == 0L) {
        stringResource(
            R.string.split_bill_items_closed,
            money.toText(status.itemsMinor),
            money.toText(status.serviceMinor),
            money.toText(status.couvertMinor),
        )
    } else {
        stringResource(
            R.string.split_bill_items_closed_extra,
            money.toText(status.itemsMinor),
            money.toText(status.serviceMinor),
            money.toText(status.couvertMinor),
            money.toSignedText(status.extraMinor),
        )
    }
}

@Composable
private fun sumTextOf(
    sumMinor: Long,
    totalMinor: Long,
    isFromReceipt: Boolean,
    money: MoneyFormatter,
): String = stringResource(
    if (isFromReceipt) R.string.split_bill_items_sum_receipt else R.string.split_bill_items_sum,
    money.toText(sumMinor),
    money.toText(totalMinor),
)

// Why: "6 × R$ 14,00 · Por unidade", or "Sem ninguém" in red for an item nobody is marked in.
@Composable
internal fun itemRowSubtitleOf(
    row: ItemsItemRow,
    money: MoneyFormatter,
): String {
    val how = when {
        row.isLoose -> stringResource(R.string.split_bill_items_row_nobody)

        row.item.mode == ItemSplitMode.EQUAL -> pluralStringResource(
            R.plurals.split_bill_items_row_equal,
            row.consumers.size,
            row.consumers.size,
        )

        row.item.mode == ItemSplitMode.UNITS -> stringResource(R.string.split_bill_items_row_units)

        row.item.mode == ItemSplitMode.SHARES -> stringResource(R.string.split_bill_items_row_shares)

        row.item.mode == ItemSplitMode.AMOUNT -> stringResource(R.string.split_bill_items_row_amount)

        else -> stringResource(R.string.split_bill_items_row_percent)
    }
    return stringResource(
        R.string.split_bill_items_row_sub,
        row.item.quantity,
        money.toText(row.item.unitPriceMinor),
        how,
    )
}

// Why: "R$ 120,00 em itens + R$ 14,50 de serviço e couvert", or only the couvert before anything is marked for them.
@Composable
internal fun itemsPersonSubtitleOf(
    row: ItemsPersonRow,
    money: MoneyFormatter,
): String = when {
    row.itemsMinor > 0 -> stringResource(
        R.string.split_bill_items_person_sub,
        money.toText(row.itemsMinor),
        money.toText(row.feesMinor),
    )

    row.feesMinor > 0 -> stringResource(R.string.split_bill_items_person_couvert_only)

    else -> stringResource(R.string.split_bill_items_person_nothing)
}

@Composable
internal fun itemStatusTitleOf(
    status: ItemStatus,
    money: MoneyFormatter,
): String = when (status) {
    ItemStatus.Nobody -> stringResource(R.string.split_bill_item_nobody)

    is ItemStatus.UnitsAddUp -> pluralStringResource(
        R.plurals.split_bill_item_units_closed,
        status.quantity.toInt(),
        status.quantity,
        status.quantity,
    )

    is ItemStatus.UnitsLeft -> pluralStringResource(
        R.plurals.split_bill_item_units_left,
        status.missing.toInt(),
        status.missing,
    )

    is ItemStatus.UnitsOver -> pluralStringResource(
        R.plurals.split_bill_item_units_over,
        status.over.toInt(),
        status.over,
    )

    is ItemStatus.Each -> stringResource(R.string.split_bill_status_each, money.toText(status.eachMinor))

    is ItemStatus.Parts -> pluralStringResource(
        R.plurals.split_bill_status_parts,
        status.parts.toInt(),
        status.parts,
        money.toText(status.partMinor),
    )

    is ItemStatus.AmountAddsUp -> stringResource(
        R.string.split_bill_status_amount_closed,
        money.toText(status.totalMinor),
    )

    is ItemStatus.AmountLeft -> stringResource(R.string.split_bill_status_amount_left, money.toText(status.amountMinor))

    is ItemStatus.AmountOver -> stringResource(R.string.split_bill_status_amount_over, money.toText(status.amountMinor))

    is ItemStatus.PercentAddsUp -> stringResource(R.string.split_bill_status_percent_closed, FULL_PERCENT)

    is ItemStatus.PercentLeft -> stringResource(R.string.split_bill_status_percent_left, status.percent)

    is ItemStatus.PercentOver -> stringResource(R.string.split_bill_status_percent_over, status.percent)
}

@Composable
internal fun itemStatusDetailOf(
    status: ItemStatus,
    money: MoneyFormatter,
): String = when (status) {
    ItemStatus.Nobody -> stringResource(R.string.split_bill_item_nobody_sub)

    is ItemStatus.UnitsAddUp -> stringResource(
        R.string.split_bill_item_units_price,
        money.toText(status.unitPriceMinor),
    )

    is ItemStatus.UnitsLeft -> pluralStringResource(
        R.plurals.split_bill_item_units_left_sub,
        status.missing.toInt(),
        status.marked,
        status.quantity,
    )

    is ItemStatus.UnitsOver -> stringResource(R.string.split_bill_item_units_marked, status.marked, status.quantity)

    is ItemStatus.Each -> amongTextOf(status.totalMinor, status.count, money)

    is ItemStatus.Parts ->
        status.severalParts
            .map { parts ->
                if (parts.person.isMe) {
                    pluralStringResource(R.plurals.split_bill_item_my_parts, parts.parts.toInt(), parts.parts)
                } else {
                    pluralStringResource(
                        R.plurals.split_bill_item_person_parts,
                        parts.parts.toInt(),
                        parts.person.name,
                        parts.parts,
                    )
                }
            }.ifEmpty { listOf(amongTextOf(status.totalMinor, status.count, money)) }
            .joinToString(SENTENCE_SEPARATOR)

    is ItemStatus.AmountAddsUp -> stringResource(R.string.split_bill_item_amount_closed)

    is ItemStatus.AmountLeft -> stringResource(
        R.string.split_bill_status_amount_progress,
        money.toText(status.assignedMinor),
        money.toText(status.totalMinor),
    )

    is ItemStatus.AmountOver -> stringResource(
        R.string.split_bill_status_amount_progress,
        money.toText(status.assignedMinor),
        money.toText(status.totalMinor),
    )

    is ItemStatus.PercentAddsUp -> amongTextOf(status.totalMinor, status.count, money)

    is ItemStatus.PercentLeft -> stringResource(R.string.split_bill_item_percent_progress, status.sum)

    is ItemStatus.PercentOver -> stringResource(R.string.split_bill_item_percent_progress, status.sum)
}

@Composable
private fun amongTextOf(
    totalMinor: Long,
    count: Int,
    money: MoneyFormatter,
): String = pluralStringResource(R.plurals.split_bill_status_among, count, money.toText(totalMinor), count)

@Composable
internal fun itemModeLabelOf(mode: ItemSplitMode): String = stringResource(
    when (mode) {
        ItemSplitMode.EQUAL -> R.string.split_bill_item_mode_equal
        ItemSplitMode.UNITS -> R.string.split_bill_item_mode_units
        ItemSplitMode.SHARES -> R.string.split_bill_item_mode_shares
        ItemSplitMode.AMOUNT -> R.string.split_bill_item_mode_amount
        ItemSplitMode.PERCENT -> R.string.split_bill_item_mode_percent
    },
)

@Composable
internal fun itemListLabelOf(mode: ItemSplitMode): String = stringResource(
    when (mode) {
        ItemSplitMode.EQUAL -> R.string.split_bill_item_list_equal
        ItemSplitMode.UNITS -> R.string.split_bill_item_list_units
        ItemSplitMode.SHARES -> R.string.split_bill_item_list_shares
        ItemSplitMode.AMOUNT -> R.string.split_bill_item_list_amount
        ItemSplitMode.PERCENT -> R.string.split_bill_item_list_percent
    },
)

@Composable
internal fun itemHintOf(
    mode: ItemSplitMode,
    money: MoneyFormatter,
): String = when (mode) {
    ItemSplitMode.EQUAL -> stringResource(R.string.split_bill_item_hint_equal)
    ItemSplitMode.UNITS -> stringResource(R.string.split_bill_item_hint_units)
    ItemSplitMode.SHARES -> stringResource(R.string.split_bill_item_hint_shares)
    ItemSplitMode.AMOUNT -> stringResource(R.string.split_bill_item_hint_amount, money.toText(0L))
    ItemSplitMode.PERCENT -> stringResource(R.string.split_bill_item_hint_percent)
}

// Why: "“Cerveja long neck” (R$ 84,00) sai da conta e o serviço é refeito. Os itens deixam de fechar com o total: …"
@Composable
internal fun itemDeletionTextOf(
    deletion: ItemDeletion,
    money: MoneyFormatter,
): String {
    val first = stringResource(
        if (deletion.redoesService) {
            R.string.split_bill_item_delete_text_service
        } else {
            R.string.split_bill_item_delete_text
        },
        deletion.name,
        money.toText(deletion.totalMinor),
    )
    val left = deletion.leftMinor?.let { stringResource(R.string.split_bill_item_delete_left, money.toText(it)) }
    return listOfNotNull(first, left).joinToString(SENTENCE_SEPARATOR)
}

package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Why: what a receipt read fills in (7.1e): the amount, the place as the description and the day it was printed,
 * leaving whatever it didn't read as it was, and whatever was typed while it read (`beforeRead`, the form when the
 * photo was taken). The split stays as it is; the items wait for "Itens".
 */
internal fun SplitBillUiState.withReceiptRead(
    reading: ReceiptReading,
    beforeRead: SplitBillUiState,
    decimalSeparator: Char,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val fillsAmount = amountText == beforeRead.amountText
    val description = reading.description?.takeIf { this.description == beforeRead.description }
    val filled = copy(
        amountText = if (fillsAmount) {
            AmountInput.format(
                amountMinor = reading.totalMinor,
                currency = currency,
                decimalSeparator = decimalSeparator,
            )
        } else {
            amountText
        },
        hasEditedAmount = true,
        description = description ?: this.description,
        hasEditedDescription = hasEditedDescription || description != null,
        receiptCategoryId = reading.categoryId,
    )
    val date = reading.date?.takeIf { spentAt == beforeRead.spentAt } ?: return filled
    // Why: a day printed without its time keeps the time "Quando" had; a later time today becomes now.
    val time = reading.time ?: Instant.fromEpochMilliseconds(spentAt).toLocalDateTime(timeZone).time
    val printedAt = LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
    return filled.withSpentAt(printedAt, now, timeZone)
}

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
 * leaving whatever it didn't read as it was. The split stays as it is; the items wait for "Itens".
 */
internal fun SplitBillUiState.withReceiptRead(
    reading: ReceiptReading,
    decimalSeparator: Char,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val description = reading.description
    val filled = copy(
        amountText = AmountInput.format(
            amountMinor = reading.totalMinor,
            currency = currency,
            decimalSeparator = decimalSeparator,
        ),
        hasEditedAmount = true,
        description = description ?: this.description,
        hasEditedDescription = hasEditedDescription || description != null,
        receiptCategoryId = reading.categoryId,
    )
    val date = reading.date ?: return filled
    // Why: a day printed without its time keeps the time "Quando" had; a later time today becomes now.
    val time = reading.time ?: Instant.fromEpochMilliseconds(spentAt).toLocalDateTime(timeZone).time
    val printedAt = LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
    return filled.withSpentAt(printedAt, now, timeZone)
}

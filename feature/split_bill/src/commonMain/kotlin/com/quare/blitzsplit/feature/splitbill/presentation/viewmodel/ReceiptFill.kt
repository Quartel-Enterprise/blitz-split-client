package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/*
 * Why: what a receipt read fills in (7.1e): the amount, the place as the description and the day it was printed,
 * leaving whatever it didn't read as it was. Nothing was typed meanwhile, since the form is locked while it reads
 * (`SplitBillUiState.isFormLocked`). The split stays as it is; the items wait for "Itens".
 */
internal fun SplitBillUiState.withReceiptRead(
    reading: ReceiptReading,
    decimalSeparator: Char,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val filled = copy(
        amountText = AmountInput.format(
            amountMinor = reading.totalMinor,
            currency = currency,
            showsSats = showsSats,
            decimalSeparator = decimalSeparator,
        ),
        hasEditedAmount = true,
        description = reading.description ?: description,
        hasEditedDescription = hasEditedDescription || reading.description != null,
        receiptCategoryId = reading.categoryId,
    )
    val date = reading.date ?: return filled
    // Why: a day printed without its time keeps the time "Quando" had, or none; a later time today becomes now.
    val time = reading.time ?: return filled.withSpentDayPicked(date, now, timeZone)
    val printedAt = LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
    return filled.withSpentAt(printedAt, now, timeZone)
}

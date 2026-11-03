package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.model.GroupCurrency

fun interface ReceiptMapper {
    // Why: null when the total can't be saved as a bill in the group's currency, which reads as an unreadable receipt.
    fun toDomain(
        dto: ReadReceiptResponseDto,
        currency: GroupCurrency,
    ): ReceiptReading?
}

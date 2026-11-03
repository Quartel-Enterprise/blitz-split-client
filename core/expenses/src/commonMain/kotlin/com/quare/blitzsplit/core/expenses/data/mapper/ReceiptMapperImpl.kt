package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptItemDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/*
 * Why: the server already keeps a reading within the app's rules; this keeps them once more, so a reading of another
 * version of the server never fills Rachar conta with something it can't save.
 */
class ReceiptMapperImpl : ReceiptMapper {
    override fun toDomain(
        dto: ReadReceiptResponseDto,
        currency: GroupCurrency,
        groupCategoryIds: Set<String>,
    ): ReceiptReading? {
        if (!ExpenseRules.isAmountValid(dto.totalMinor, currency)) return null
        val date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return ReceiptReading(
            totalMinor = dto.totalMinor,
            description = dto.description?.let(::toDescriptionOrNull),
            categoryId = dto.category?.takeIf { AppCategory.fromKeyOrNull(it) != null || it in groupCategoryIds },
            date = date,
            time = dto.time?.takeIf { date != null }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            items = dto.items
                .orEmpty()
                .mapNotNull(::toItem)
                .take(BillItemsSplit.MAX_ITEMS),
            servicePercent = (dto.servicePercent ?: 0L).coerceIn(0L, BillItemsSplit.MAX_SERVICE_PERCENT),
        )
    }

    private fun toDescriptionOrNull(text: String): String? = ExpenseRules
        .toCapped(text.trim())
        .trim()
        .takeIf { it.isNotEmpty() }

    private fun toItem(dto: ReadReceiptItemDto): ReceiptItem? {
        val name = ExpenseRules.toCapped(dto.name.trim(), BillItemsSplit.NAME_MAX_LENGTH).trim()
        if (name.isEmpty() || dto.quantity !in 1..BillItemsSplit.MAX_QUANTITY || dto.unitPriceMinor <= 0) return null
        return ReceiptItem(
            name = name,
            quantity = dto.quantity,
            unitPriceMinor = dto.unitPriceMinor,
        )
    }
}

package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity

interface ExpenseMapper {
    // Why: null for a row this version can't read (a split mode added later), which the list skips.
    fun toDomain(entity: ExpenseEntity): Expense?

    fun toEntity(
        expense: Expense,
        createdAt: Long,
    ): ExpenseEntity

    fun toEntity(dto: ExpenseDto): ExpenseEntity

    fun toDto(
        entity: ExpenseEntity,
        userId: String,
    ): ExpenseDto
}

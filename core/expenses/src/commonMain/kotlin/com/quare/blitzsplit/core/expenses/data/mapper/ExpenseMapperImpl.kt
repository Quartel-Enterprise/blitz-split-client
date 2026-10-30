package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import kotlinx.serialization.json.Json

class ExpenseMapperImpl : ExpenseMapper {
    private val json = Json { ignoreUnknownKeys = true }

    override fun toDomain(entity: ExpenseEntity): Expense? {
        val splitMode = SplitMode.fromCodeOrNull(entity.splitMode) ?: return null
        return Expense(
            id = entity.id,
            groupId = entity.groupId,
            description = entity.description,
            amountMinor = entity.amountMinor,
            splitMode = splitMode,
            payers = decodePayers(entity.payersJson).map {
                ExpensePayer(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                )
            },
            shares = decodeShares(entity.sharesJson).map {
                ExpenseShare(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                    weight = it.weight,
                )
            },
            spentAt = entity.spentAt,
            isPending = entity.pendingSync,
        )
    }

    override fun toEntity(
        expense: Expense,
        createdAt: Long,
    ): ExpenseEntity = ExpenseEntity(
        id = expense.id,
        groupId = expense.groupId,
        createdBy = null,
        description = expense.description,
        amountMinor = expense.amountMinor,
        splitMode = expense.splitMode.code,
        payersJson = json.encodeToString(
            expense.payers.map {
                ExpensePayerDto(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                )
            },
        ),
        sharesJson = json.encodeToString(
            expense.shares.map {
                ExpenseShareDto(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                    weight = it.weight,
                )
            },
        ),
        spentAt = expense.spentAt,
        isDeleted = false,
        addedAt = createdAt,
        updatedAt = createdAt,
        pendingSync = true,
    )

    override fun toEntity(dto: ExpenseDto): ExpenseEntity = ExpenseEntity(
        id = dto.id,
        groupId = dto.groupId,
        createdBy = dto.createdBy,
        description = dto.description,
        amountMinor = dto.amountMinor,
        splitMode = dto.splitMode,
        payersJson = json.encodeToString(dto.payers),
        sharesJson = json.encodeToString(dto.shares),
        spentAt = dto.spentAt,
        isDeleted = dto.isDeleted,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
    )

    // Why: a bill created before any account goes up as created by the user the sync runs under.
    override fun toDto(
        entity: ExpenseEntity,
        userId: String,
    ): ExpenseDto = ExpenseDto(
        id = entity.id,
        groupId = entity.groupId,
        createdBy = entity.createdBy ?: userId,
        description = entity.description,
        amountMinor = entity.amountMinor,
        splitMode = entity.splitMode,
        payers = decodePayers(entity.payersJson),
        shares = decodeShares(entity.sharesJson),
        spentAt = entity.spentAt,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
    )

    private fun decodePayers(text: String): List<ExpensePayerDto> = json.decodeFromString(text)

    private fun decodeShares(text: String): List<ExpenseShareDto> = json.decodeFromString(text)
}

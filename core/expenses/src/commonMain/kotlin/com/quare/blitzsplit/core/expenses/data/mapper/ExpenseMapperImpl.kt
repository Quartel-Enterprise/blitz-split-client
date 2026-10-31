package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ExpenseMapperImpl : ExpenseMapper {
    private val json = Json { ignoreUnknownKeys = true }

    // Why: null for a bill this version can't read (a split mode or payers it doesn't know), which the list skips.
    override fun toDomain(row: ExpenseWithEditor): Expense? {
        val entity = row.expense
        val splitMode = SplitMode.fromCodeOrNull(entity.splitMode) ?: return null
        val payers = decodeOrNull { decodePayers(entity.payersJson) } ?: return null
        val shares = decodeOrNull { decodeShares(entity.sharesJson) } ?: return null
        return Expense(
            id = entity.id,
            groupId = entity.groupId,
            description = entity.description,
            amountMinor = entity.amountMinor,
            splitMode = splitMode,
            payers = payers.map {
                ExpensePayer(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                )
            },
            shares = shares.map {
                ExpenseShare(
                    memberId = it.memberId,
                    amountMinor = it.amountMinor,
                    weight = it.weight,
                )
            },
            spentAt = entity.spentAt,
            isPending = entity.pendingSync,
            updatedAt = entity.updatedAt,
            updatedByMemberId = row.updatedByMemberId,
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
        updatedBy = null,
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
        updatedBy = dto.updatedBy,
        pendingSync = false,
    )

    /*
     * Why: an upsert is checked against the server's insert policy as sent, also for a bill that exists, so it
     * always goes up as created by whoever pushes; the server keeps who really created it.
     */
    override fun toDto(
        entity: ExpenseEntity,
        userId: String,
    ): ExpenseDto = ExpenseDto(
        id = entity.id,
        groupId = entity.groupId,
        createdBy = userId,
        description = entity.description,
        amountMinor = entity.amountMinor,
        splitMode = entity.splitMode,
        payers = decodePayers(entity.payersJson),
        shares = decodeShares(entity.sharesJson),
        spentAt = entity.spentAt,
        isDeleted = entity.isDeleted,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
        // Why: the server records the session's user anyway, which is the one pushing.
        updatedBy = userId,
    )

    private fun <T> decodeOrNull(decode: () -> T): T? = try {
        decode()
    } catch (_: SerializationException) {
        null
    }

    private fun decodePayers(text: String): List<ExpensePayerDto> = json.decodeFromString(text)

    private fun decodeShares(text: String): List<ExpenseShareDto> = json.decodeFromString(text)
}

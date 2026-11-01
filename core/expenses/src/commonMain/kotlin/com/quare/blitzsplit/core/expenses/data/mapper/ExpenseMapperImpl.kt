package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseItemDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseItemEntryDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseItemsDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpensePayerDto
import com.quare.blitzsplit.core.expenses.data.dto.ExpenseShareDto
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ExpenseMapperImpl : ExpenseMapper {
    private val json = Json { ignoreUnknownKeys = true }

    /*
     * Why: null for a bill this version can't read (a split mode or payers it doesn't know), which the list skips.
     * Items that don't give the stored shares (malformed, or a mode this version doesn't know) leave the bill split by
     * amounts, which its shares still are.
     */
    override fun toDomain(row: ExpenseWithEditor): Expense? {
        val entity = row.expense
        val storedMode = SplitMode.fromCodeOrNull(entity.splitMode)?.takeIf { it != SplitMode.ITEMS } ?: return null
        val payerDtos = decodeOrNull { decodePayers(entity.payersJson) } ?: return null
        val shareDtos = decodeOrNull { decodeShares(entity.sharesJson) } ?: return null
        val payers = payerDtos.map {
            ExpensePayer(
                memberId = it.memberId,
                amountMinor = it.amountMinor,
            )
        }
        val shares = shareDtos.map {
            ExpenseShare(
                memberId = it.memberId,
                amountMinor = it.amountMinor,
                weight = it.weight,
            )
        }
        val items = entity.itemsJson
            ?.takeIf { storedMode == SplitMode.AMOUNT }
            ?.let { text -> decodeOrNull { decodeItems(text) } }
            ?.let(::toDomainOrNull)
            ?.takeIf { BillItemsSplit.sharesOf(it, payers.map { payer -> payer.memberId }) == shares }
        return Expense(
            id = entity.id,
            groupId = entity.groupId,
            description = entity.description,
            amountMinor = entity.amountMinor,
            splitMode = if (items != null) SplitMode.ITEMS else storedMode,
            payers = payers,
            shares = shares,
            items = items,
            categoryId = entity.categoryId,
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
        splitMode = SplitMode.storedCodeOf(expense.splitMode),
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
        itemsJson = expense.items?.let { json.encodeToString(toDto(it, updatedAt = createdAt)) },
        categoryId = expense.categoryId,
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
        // Why: items another version saved the bill without (an older updated_at) no longer give its shares.
        itemsJson = dto.items?.takeIf { it.updatedAt == dto.updatedAt }?.let { json.encodeToString(it) },
        categoryId = dto.categoryId,
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
        // Why: stamped with the version they go up with, which tells the server they come with these shares.
        items = entity.itemsJson
            ?.let { text -> decodeOrNull { decodeItems(text) } }
            ?.copy(updatedAt = entity.updatedAt),
        categoryId = entity.categoryId,
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

    private fun decodeItems(text: String): ExpenseItemsDto = json.decodeFromString(text)

    private fun toDomainOrNull(dto: ExpenseItemsDto): BillItems? {
        val items = dto.items.map { item ->
            BillItem(
                id = item.id,
                name = item.name,
                quantity = item.quantity,
                unitPriceMinor = item.unitPriceMinor,
                mode = ItemSplitMode.fromCodeOrNull(item.mode) ?: return null,
                entries = item.entries.map {
                    SplitEntry(
                        memberId = it.memberId,
                        value = it.value,
                    )
                },
            )
        }
        return BillItems(
            items = items,
            servicePercent = dto.servicePercent,
            couvertMinor = dto.couvertMinor,
            couvertMemberIds = dto.couvertMemberIds,
            extraMinor = dto.extraMinor,
            isFromReceipt = dto.fromReceipt,
        )
    }

    private fun toDto(
        split: BillItems,
        updatedAt: Long,
    ): ExpenseItemsDto = ExpenseItemsDto(
        updatedAt = updatedAt,
        items = split.items.map { item ->
            ExpenseItemDto(
                id = item.id,
                name = item.name,
                quantity = item.quantity,
                unitPriceMinor = item.unitPriceMinor,
                mode = item.mode.code,
                entries = item.entries.map {
                    ExpenseItemEntryDto(
                        memberId = it.memberId,
                        value = it.value,
                    )
                },
            )
        },
        servicePercent = split.servicePercent,
        couvertMinor = split.couvertMinor,
        couvertMemberIds = split.couvertMemberIds,
        extraMinor = split.extraMinor,
        fromReceipt = split.isFromReceipt,
    )
}

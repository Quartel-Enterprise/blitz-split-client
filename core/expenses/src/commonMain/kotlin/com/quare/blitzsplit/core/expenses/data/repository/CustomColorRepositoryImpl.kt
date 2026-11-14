package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.CustomColorLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.CustomColorRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.CustomColorMapper
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CustomColorRepositoryImpl(
    private val local: CustomColorLocalDataSource,
    private val remote: CustomColorRemoteDataSource,
    private val mapper: CustomColorMapper,
) : CustomColorRepository {
    // Why: a row this version can't read (a shape it doesn't know) is left out rather than failing the palette.
    override fun observeColors(): Flow<List<CustomColor>> = local
        .observeAll()
        .map { entities ->
            entities.filterNot { it.isDeleted }.mapNotNull(mapper::toDomainOrNull)
        }.distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observeVersions(): Flow<List<Long>> = local
        .observeAll()
        .map { entities -> entities.map { it.updatedAt } }
        .distinctUntilChanged()

    /*
     * Why: one row per color, which a change stamps past the version it read, so it wins even when this device's clock
     * is behind the one that saved it last. A color still in the palette is left alone, and one added again keeps its
     * first place, as the server keeps the earliest added_at.
     */
    override suspend fun add(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val stored = local.getByColor(color.hex)
        if (stored != null && !stored.isDeleted) return@suspendRunCatching
        local.upsert(
            listOf(
                stored?.copy(
                    isDeleted = false,
                    updatedAt = maxOf(updatedAt, stored.updatedAt + 1),
                    pendingSync = true,
                ) ?: CustomColorEntity(
                    color = color.hex,
                    isDeleted = false,
                    addedAt = updatedAt,
                    updatedAt = updatedAt,
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )
    }

    override suspend fun delete(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val stored = local.getByColor(color.hex) ?: return@suspendRunCatching
        if (stored.isDeleted) return@suspendRunCatching
        local.upsert(
            listOf(
                stored.copy(
                    isDeleted = true,
                    updatedAt = maxOf(updatedAt, stored.updatedAt + 1),
                    pendingSync = true,
                ),
            ),
        )
    }

    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPending(userId).ifEmpty { return@suspendRunCatching }
        remote.upsert(pending.map { mapper.toDto(it, userId) })
        // Why: once up, a color made before the account is this user's, and stays pending if changed meanwhile.
        local.claimUnowned(userId)
        pending.forEach { local.markSynced(it.color, it.updatedAt) }
    }

    // Why: Last-Write-Wins per color, as the server keeps it: a newer row wins even over a change not pushed yet.
    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val rows = remote.fetch(userId)
        local.claimUnowned(userId)
        local.deleteOtherUsersColors(userId)
        val stored = local.getAll().associateBy(CustomColorEntity::color)
        val newer = rows
            .mapNotNull { mapper.toEntityOrNull(it, userId) }
            .filter { row -> stored[row.color].let { it == null || it.updatedAt < row.updatedAt } }
        if (newer.isNotEmpty()) local.upsert(newer)
    }

    override suspend fun clear() {
        local.clear()
    }
}

package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.CategoryTermLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.CategoryTermRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.CategoryTermMapper
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CategoryTermRepositoryImpl(
    private val local: CategoryTermLocalDataSource,
    private val remote: CategoryTermRemoteDataSource,
    private val mapper: CategoryTermMapper,
) : CategoryTermRepository {
    override fun observeChanges(): Flow<List<CategoryTermChange>> = local
        .observeAll()
        .map { entities -> entities.map(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    /*
     * Why: one row per word, which an edit stamps past the version it read, so it wins even when this device's clock
     * is behind the one that saved it last.
     */
    override suspend fun saveTerm(
        term: String,
        categoryId: String?,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val trimmed = term.trim()
        val key = CategoryMatcher.keyOf(trimmed)
        val stored = local.getByKey(key)
        local.upsert(
            listOf(
                stored?.copy(
                    term = trimmed,
                    categoryId = categoryId,
                    isDeleted = false,
                    updatedAt = maxOf(updatedAt, stored.updatedAt + 1),
                    pendingSync = true,
                ) ?: CategoryTermEntity(
                    termKey = key,
                    term = trimmed,
                    categoryId = categoryId,
                    isDeleted = false,
                    addedAt = updatedAt,
                    updatedAt = updatedAt,
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )
    }

    override suspend fun restoreAppTerms(
        appKeys: Set<String>,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val restored = local
            .getAll()
            .filter { it.termKey in appKeys && !it.isDeleted }
            .map {
                it.copy(
                    isDeleted = true,
                    updatedAt = maxOf(updatedAt, it.updatedAt + 1),
                    pendingSync = true,
                )
            }
        if (restored.isNotEmpty()) local.upsert(restored)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPending(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsert(pending.map { mapper.toDto(it, userId) })
        local.claimUnowned(userId)
        pending.forEach { local.markSynced(it.termKey, it.updatedAt) }
    }

    // Why: Last-Write-Wins per word, as the server keeps it: a newer row wins even over a change not pushed yet.
    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val rows = remote.fetch(userId)
        local.deleteOtherUsersTerms(userId)
        local.claimUnowned(userId)
        val stored = local.getAll().associateBy { it.termKey }
        val newer = rows
            .filter { dto -> stored[dto.termKey].let { it == null || it.updatedAt < dto.updatedAt } }
            .map { mapper.toEntity(it, userId) }
        if (newer.isNotEmpty()) local.upsert(newer)
    }

    override suspend fun clear() {
        local.clear()
    }
}

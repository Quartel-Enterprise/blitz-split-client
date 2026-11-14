package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.PayingMethodLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.PayingMethodRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.PayingMethodMapper
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PayingMethodRepositoryImpl(
    private val local: PayingMethodLocalDataSource,
    private val remote: PayingMethodRemoteDataSource,
    private val mapper: PayingMethodMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : PayingMethodRepository {
    override fun observeMethods(): Flow<List<PayingMethod>> = local
        .observeLiveMethods()
        .map(mapper::toDomain)
        .distinctUntilChanged()

    override fun observeMarks(): Flow<Map<String, String>> = local
        .observeMarks()
        .map { marks -> marks.mapNotNull { mark -> mark.methodId?.let { mark.expenseId to it } }.toMap() }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    // Why: stamped past the version each method had, so an edit wins even when this device's clock is behind.
    override suspend fun saveMethods(methods: List<PayingMethod>) {
        val stored = local.getAllMethods().associateBy { it.id }
        val now = currentTimeMillis()
        local.upsertMethods(
            methods.map { method ->
                val previous = stored[method.id]
                mapper.toEntity(
                    method = method,
                    updatedAt = maxOf(now, (previous?.updatedAt ?: 0L) + 1),
                    ownerUserId = previous?.ownerUserId,
                )
            },
        )
    }

    // Why: a tombstone without its name, so the deletion reaches the person's other devices and no name stays behind.
    override suspend fun deleteMethod(id: String) {
        val stored = local.getAllMethods().firstOrNull { it.id == id && !it.isDeleted } ?: return
        local.upsertMethods(
            listOf(
                stored.copy(
                    name = "",
                    icon = "",
                    isDefault = false,
                    isDeleted = true,
                    updatedAt = maxOf(currentTimeMillis(), stored.updatedAt + 1),
                    pendingSync = true,
                ),
            ),
        )
    }

    override suspend fun markBill(
        expenseId: String,
        methodId: String?,
    ) {
        val stored = local.getAllMarks().firstOrNull { it.expenseId == expenseId }
        if (stored?.methodId == methodId || (stored == null && methodId == null)) return
        local.upsertMarks(
            listOf(
                BillPayingMethodEntity(
                    expenseId = expenseId,
                    methodId = methodId,
                    updatedAt = maxOf(currentTimeMillis(), (stored?.updatedAt ?: 0L) + 1),
                    pendingSync = true,
                    ownerUserId = stored?.ownerUserId,
                ),
            ),
        )
    }

    override suspend fun sync(
        userId: String,
        withMarks: Boolean,
    ): Result<Unit> = suspendRunCatching {
        local.keepOnlyUser(userId)
        downloadMethods(userId)
        uploadMethods(userId)
        if (withMarks) {
            downloadMarks(userId)
            uploadMarks(userId)
        }
    }

    /*
     * Why: Last-Write-Wins per method, as the server keeps it, and a deletion is final on both sides: a tombstone wins
     * whatever its age, and nothing brings back a method deleted here.
     */
    private suspend fun downloadMethods(userId: String) {
        val stored = local.getAllMethods().associateBy { it.id }
        val newer = remote
            .fetchMethods(userId)
            .filter { dto ->
                val mine = stored[dto.id]
                mine == null && !dto.isDeleted ||
                    mine != null && !mine.isDeleted && (dto.isDeleted || mine.updatedAt < dto.updatedAt)
            }.map { mapper.toEntity(it, userId) }
        if (newer.isNotEmpty()) local.upsertMethods(newer)
    }

    private suspend fun uploadMethods(userId: String) {
        val pending = local.getAllMethods().filter { it.pendingSync }
        if (pending.isNotEmpty()) {
            remote.upsertMethods(pending.map { mapper.toDto(it, userId) })
            pending.forEach { local.markMethodSynced(it.id, it.updatedAt) }
        }
        local.deleteSyncedTombstones()
    }

    // Why: Last-Write-Wins per bill; a mark whose bill isn't on this device (a group left) is dropped as it comes.
    private suspend fun downloadMarks(userId: String) {
        val stored = local.getAllMarks().associateBy { it.expenseId }
        val newer = remote
            .fetchMarks(userId)
            .filter { dto -> stored[dto.expenseId].let { it == null || it.updatedAt < dto.updatedAt } }
            .map { mapper.toEntity(it, userId) }
        if (newer.isNotEmpty()) local.upsertMarks(newer)
        local.deleteOrphanMarks()
    }

    private suspend fun uploadMarks(userId: String) {
        val ready = local.getMarksReadyToUpload()
        if (ready.isEmpty()) return
        remote.upsertMarks(ready.map { mapper.toDto(it, userId) })
        ready.forEach { local.markMarkSynced(it.expenseId, it.updatedAt) }
    }

    override suspend fun clear() {
        local.clear()
    }
}

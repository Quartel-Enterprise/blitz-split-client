package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PaymentKeyRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.mapper.PaymentKeyMapper
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PaymentKeyRepositoryImpl(
    private val local: PaymentKeyLocalDataSource,
    private val remote: PaymentKeyRemoteDataSource,
    private val profileLocal: ProfileLocalDataSource,
    private val mapper: PaymentKeyMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : PaymentKeyRepository {
    override fun observeKeys(): Flow<List<PaymentKey>> = local
        .observeLive()
        .map(mapper::toDomain)
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    /*
     * Why: a new key belongs to whoever the profile belongs to, so a switch drops it with the profile's copy; an
     * edited one keeps its owner, so an edit to another user's key still gives way.
     */
    override suspend fun saveKeys(keys: List<PaymentKey>) {
        val stored = local.getAll().associateBy { it.id }
        val profileOwner = profileLocal.get()?.ownerUserId
        val updatedAt = currentTimeMillis()
        local.upsert(
            keys.map { key ->
                mapper.toEntity(
                    key = key,
                    updatedAt = updatedAt,
                    ownerUserId = stored[key.id]?.ownerUserId ?: profileOwner,
                )
            },
        )
    }

    // Why: a tombstone, so the deletion reaches the server and from there the user's other devices.
    override suspend fun deleteKey(id: String) {
        val stored = local.getAll().find { it.id == id } ?: return
        local.upsert(
            listOf(
                stored.copy(
                    isDefault = false,
                    isDeleted = true,
                    updatedAt = currentTimeMillis(),
                    pendingSync = true,
                ),
            ),
        )
    }

    override suspend fun uploadPendingKeys(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getAll().filter { it.pendingSync && it.ownerUserId == userId }
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsert(
            pending.map {
                mapper.toDto(
                    entity = it,
                    userId = userId,
                )
            },
        )
        pending.forEach {
            local.markSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
            )
        }
        local.deleteSyncedTombstones()
    }

    /*
     * Why: unlike the profile, another user's keys never go up as this account's, even when it has none: they
     * are someone's CPF or wallet. Keys made before any account are this user's.
     */
    override suspend fun downloadKeys(userId: String): Result<Unit> = suspendRunCatching {
        val remoteKeys = remote.fetch(userId)
        local.deleteOtherUsersKeys(userId)
        local.claimUnowned(userId)
        remoteKeys.forEach { dto ->
            local.applyRemote(
                mapper.toEntity(
                    dto = dto,
                    ownerUserId = userId,
                ),
            )
        }
        local.deleteSyncedTombstones()
    }

    override suspend fun clear() {
        local.clear()
    }
}

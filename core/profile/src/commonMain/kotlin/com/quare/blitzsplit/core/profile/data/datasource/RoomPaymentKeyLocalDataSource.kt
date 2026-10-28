package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.PaymentKeyDao
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import kotlinx.coroutines.flow.Flow

class RoomPaymentKeyLocalDataSource(
    private val dao: PaymentKeyDao,
) : PaymentKeyLocalDataSource {
    override fun observeLive(): Flow<List<PaymentKeyEntity>> = dao.observeLive()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getAll(): List<PaymentKeyEntity> = dao.getAll()

    override suspend fun upsert(entities: List<PaymentKeyEntity>) {
        dao.upsert(entities)
    }

    override suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markSynced(
            id = id,
            syncedUpdatedAt = syncedUpdatedAt,
        )
    }

    override suspend fun deleteSyncedTombstones() {
        dao.deleteSyncedTombstones()
    }

    // Why: a key only the server has comes down whole; one the device has follows Last-Write-Wins.
    override suspend fun applyRemote(entity: PaymentKeyEntity) {
        dao.insertIfAbsent(entity)
        dao.applyRemote(
            id = entity.id,
            type = entity.type,
            value = entity.value,
            isDefault = entity.isDefault,
            isDeleted = entity.isDeleted,
            addedAt = entity.addedAt,
            remoteUpdatedAt = entity.updatedAt,
            ownerUserId = requireNotNull(entity.ownerUserId),
        )
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsersKeys(ownerUserId: String) {
        dao.deleteOtherUsersKeys(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

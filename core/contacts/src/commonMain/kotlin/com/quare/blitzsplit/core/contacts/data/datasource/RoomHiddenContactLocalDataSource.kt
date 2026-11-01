package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.HiddenContactDao
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import kotlinx.coroutines.flow.Flow

class RoomHiddenContactLocalDataSource(
    private val dao: HiddenContactDao,
) : HiddenContactLocalDataSource {
    override fun observeAll(): Flow<List<HiddenContactEntity>> = dao.observeAll()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getAll(): List<HiddenContactEntity> = dao.getAll()

    override suspend fun getOrNull(personKey: String): HiddenContactEntity? = dao.getOrNull(personKey)

    override suspend fun upsert(entity: HiddenContactEntity) {
        dao.upsert(entity)
    }

    override suspend fun markSynced(
        personKey: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markSynced(
            personKey = personKey,
            syncedUpdatedAt = syncedUpdatedAt,
        )
    }

    // Why: a row only the server has comes down whole; one the device has follows Last-Write-Wins.
    override suspend fun applyRemote(entity: HiddenContactEntity) {
        dao.insertIfAbsent(entity)
        dao.applyRemote(
            personKey = entity.personKey,
            isHidden = entity.isHidden,
            hiddenAt = entity.hiddenAt,
            remoteUpdatedAt = entity.updatedAt,
            ownerUserId = requireNotNull(entity.ownerUserId),
        )
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsersRows(ownerUserId: String) {
        dao.deleteOtherUsersRows(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

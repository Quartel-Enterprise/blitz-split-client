package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.NotificationPreferencesDao
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import kotlinx.coroutines.flow.Flow

class RoomNotificationPreferencesLocalDataSource(
    private val dao: NotificationPreferencesDao,
) : NotificationPreferencesLocalDataSource {
    override fun observe(): Flow<NotificationPreferencesEntity?> = dao.observe()

    override suspend fun get(): NotificationPreferencesEntity? = dao.get()

    override suspend fun upsert(entity: NotificationPreferencesEntity) {
        dao.upsert(entity)
    }

    override suspend fun markSynced(syncedUpdatedAt: Long) {
        dao.markSynced(syncedUpdatedAt)
    }

    // Why: preferences only the server has come down whole; ones the device has follow Last-Write-Wins.
    override suspend fun applyRemote(entity: NotificationPreferencesEntity) {
        dao.insertIfAbsent(entity)
        dao.applyRemote(
            newExpense = entity.newExpense,
            paymentMarked = entity.paymentMarked,
            settleReminders = entity.settleReminders,
            invites = entity.invites,
            remoteUpdatedAt = entity.updatedAt,
            ownerUserId = requireNotNull(entity.ownerUserId),
        )
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsers(ownerUserId: String) {
        dao.deleteOtherUsers(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

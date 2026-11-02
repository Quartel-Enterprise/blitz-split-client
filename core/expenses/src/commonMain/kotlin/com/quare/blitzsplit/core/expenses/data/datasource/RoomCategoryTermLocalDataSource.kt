package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.CategoryTermDao
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity
import kotlinx.coroutines.flow.Flow

class RoomCategoryTermLocalDataSource(
    private val dao: CategoryTermDao,
) : CategoryTermLocalDataSource {
    override fun observeAll(): Flow<List<CategoryTermEntity>> = dao.observeAll()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getAll(): List<CategoryTermEntity> = dao.getAll()

    override suspend fun getByKey(termKey: String): CategoryTermEntity? = dao.getByKey(termKey)

    override suspend fun getPending(userId: String): List<CategoryTermEntity> = dao.getPending(userId)

    override suspend fun upsert(entities: List<CategoryTermEntity>) {
        dao.upsert(entities)
    }

    override suspend fun markSynced(
        termKey: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markSynced(termKey, syncedUpdatedAt)
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsersTerms(ownerUserId: String) {
        dao.deleteOtherUsersTerms(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

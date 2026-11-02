package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.GroupCategoryDao
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity
import kotlinx.coroutines.flow.Flow

class RoomGroupCategoryLocalDataSource(
    private val dao: GroupCategoryDao,
) : GroupCategoryLocalDataSource {
    override fun observeCategories(groupId: String): Flow<List<GroupCategoryEntity>> = dao.observeCategories(groupId)

    override fun observeAllCategories(): Flow<List<GroupCategoryEntity>> = dao.observeAllCategories()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<Long>> = dao.observePendingUpdatedAts()

    override suspend fun getPendingCategories(ownerUserId: String): List<GroupCategoryEntity> =
        dao.getPendingCategories(ownerUserId)

    override suspend fun insertCategory(entity: GroupCategoryEntity) {
        dao.insertCategoryIntoGroup(entity)
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        now: Long,
    ) {
        val changed = dao.updateCategory(
            id = id,
            name = name,
            parentId = parentId,
            icon = icon,
            now = now,
        )
        check(changed == 1) { "No category $id" }
    }

    override suspend fun markDeleted(
        id: String,
        now: Long,
    ) {
        dao.markDeleted(
            id = id,
            now = now,
        )
    }

    override suspend fun applyRemote(entities: List<GroupCategoryEntity>) {
        dao.applyRemoteToGroups(entities)
    }

    override suspend fun markSynced(pushed: List<GroupCategoryEntity>) {
        dao.markAllSynced(pushed)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCategoryIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverCategoryIds = serverCategoryIds,
        )
    }
}

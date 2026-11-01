package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity
import kotlinx.coroutines.flow.Flow

interface GroupCategoryLocalDataSource {
    fun observeCategories(groupId: String): Flow<List<GroupCategoryEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun getPendingCategories(ownerUserId: String): List<GroupCategoryEntity>

    // Why: fails when the group is no longer on the device, which a sync may have removed meanwhile.
    suspend fun insertCategory(entity: GroupCategoryEntity)

    // Why: fails when the category is gone, deleted meanwhile here or by a sync.
    suspend fun updateCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        now: Long,
    )

    // Why: the category and its subcategories; one already gone is what was asked.
    suspend fun markDeleted(
        id: String,
        now: Long,
    )

    suspend fun applyRemote(entities: List<GroupCategoryEntity>)

    suspend fun markSynced(pushed: List<GroupCategoryEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCategoryIds: Set<String>,
    )
}

package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    // Why: the group's categories, deleted ones too, so their bills still read as their parent.
    fun observeGroupCategories(groupId: String): Flow<List<GroupCategory>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: when each change waiting to go up was last made, so a new one triggers a sync after a failed push.
    fun observePendingVersions(): Flow<List<Long>>

    // Why: on the device first, offline included (7.8d); the sync takes it to the group after the group itself.
    suspend fun createGroupCategory(
        category: GroupCategory,
        createdAt: Long,
    ): Result<Unit>

    suspend fun updateGroupCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        updatedAt: Long,
    ): Result<Unit>

    // Why: a tombstone with its subcategories; their bills read as the parent, or as "Outros".
    suspend fun deleteGroupCategory(
        id: String,
        deletedAt: Long,
    ): Result<Unit>

    suspend fun uploadPending(userId: String): Result<Unit>

    // Why: after the groups' download, so a category of a group that just came down has its group on the device.
    suspend fun download(userId: String): Result<Unit>
}

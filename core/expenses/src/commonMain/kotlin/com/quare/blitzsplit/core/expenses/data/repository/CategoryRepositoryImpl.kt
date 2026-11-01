package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.GroupCategoryMapper
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val groupLocal: GroupCategoryLocalDataSource,
    private val groupRemote: GroupCategoryRemoteDataSource,
    private val groupMapper: GroupCategoryMapper,
) : CategoryRepository {
    override fun observeGroupCategories(groupId: String): Flow<List<GroupCategory>> = groupLocal
        .observeCategories(groupId)
        .map { entities -> entities.map(groupMapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = groupLocal.observeHasPending().distinctUntilChanged()

    override fun observePendingVersions(): Flow<List<Long>> = groupLocal.observePendingVersions().distinctUntilChanged()

    override suspend fun createGroupCategory(
        category: GroupCategory,
        createdAt: Long,
    ): Result<Unit> = suspendRunCatching {
        groupLocal.insertCategory(groupMapper.toEntity(category, createdAt))
    }

    override suspend fun updateGroupCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        groupLocal.updateCategory(
            id = id,
            name = name,
            parentId = parentId,
            icon = icon,
            now = updatedAt,
        )
    }

    override suspend fun deleteGroupCategory(
        id: String,
        deletedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        groupLocal.markDeleted(
            id = id,
            now = deletedAt,
        )
    }

    // Why: only the categories of this user's groups, which went up before them in the same sync.
    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = groupLocal.getPendingCategories(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        groupRemote.upsertCategories(pending.map { groupMapper.toDto(it, userId) })
        groupLocal.markSynced(pending.map { it.copy(createdBy = it.createdBy ?: userId) })
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val remote = groupRemote.fetchCategories()
        groupLocal.applyRemote(remote.map(groupMapper::toEntity))
        groupLocal.deleteGoneFromServer(
            ownerUserId = userId,
            serverCategoryIds = remote.map { it.id }.toSet(),
        )
    }
}

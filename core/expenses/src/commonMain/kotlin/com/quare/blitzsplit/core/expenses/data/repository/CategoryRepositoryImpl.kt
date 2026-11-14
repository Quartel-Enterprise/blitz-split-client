package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.AppCategoryColorLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.GroupCategoryMapper
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.provider.room.entity.AppCategoryColorEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val groupLocal: GroupCategoryLocalDataSource,
    private val groupRemote: GroupCategoryRemoteDataSource,
    private val groupMapper: GroupCategoryMapper,
    private val appColorLocal: AppCategoryColorLocalDataSource,
) : CategoryRepository {
    override fun observeGroupCategories(groupId: String): Flow<List<GroupCategory>> = groupLocal
        .observeCategories(groupId)
        .map { entities -> entities.map(groupMapper::toDomain) }
        .distinctUntilChanged()

    override fun observeAllGroupCategories(): Flow<List<GroupCategory>> = groupLocal
        .observeAllCategories()
        .map { entities -> entities.map(groupMapper::toDomain) }
        .distinctUntilChanged()

    // Why: a color this version doesn't know is left out, so the category reads as the app's default.
    override fun observeAppColors(): Flow<Map<String, CategoryTint>> = appColorLocal
        .observeColors()
        .map { entities ->
            entities
                .mapNotNull { entity ->
                    CategoryTint.fromCodeOrNull(entity.color)?.let { entity.categoryKey to it }
                }.toMap()
        }.distinctUntilChanged()

    override suspend fun saveAppCategoryColor(
        categoryKey: String,
        color: CategoryTint?,
    ): Result<Unit> = suspendRunCatching {
        if (color == null) {
            appColorLocal.clearColor(categoryKey)
        } else {
            appColorLocal.setColor(
                AppCategoryColorEntity(
                    categoryKey = categoryKey,
                    color = color.code,
                ),
            )
        }
    }

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
        color: String?,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        groupLocal.updateCategory(
            id = id,
            name = name,
            parentId = parentId,
            icon = icon,
            color = color,
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

    override suspend fun fetchGroupCategories(groupId: String): Result<suspend () -> Unit> = suspendRunCatching {
        val entities = groupRemote.fetchCategories(groupId).map(groupMapper::toEntity)
        return@suspendRunCatching { groupLocal.applyRemote(entities) }
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

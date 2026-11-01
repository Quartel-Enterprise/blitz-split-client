package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory CategoryRepository: the state is public so a test can set and read it, and the failure knobs are public vars.
class FakeCategoryRepository(
    initialGroupCategories: List<GroupCategory> = emptyList(),
) : CategoryRepository {
    val groupCategories = MutableStateFlow(initialGroupCategories)
    val hasPendingChanges = MutableStateFlow(false)
    val pendingVersions = MutableStateFlow(emptyList<Long>())
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var saveError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeGroupCategories(groupId: String): Flow<List<GroupCategory>> =
        groupCategories.map { list -> list.filter { it.groupId == groupId } }

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions

    override suspend fun createGroupCategory(
        category: GroupCategory,
        createdAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        groupCategories.update { it + category }
        return Result.success(Unit)
    }

    override suspend fun updateGroupCategory(
        id: String,
        name: String,
        parentId: String?,
        icon: String?,
        updatedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        groupCategories.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        name = name,
                        parentId = parentId,
                        icon = icon?.let(CategoryIcon::fromCodeOrNull),
                        isPending = true,
                    )
                } else {
                    it
                }
            }
        }
        return Result.success(Unit)
    }

    // Why: like Room, its subcategories go with it.
    override suspend fun deleteGroupCategory(
        id: String,
        deletedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        groupCategories.update { list ->
            list.map {
                if (it.id == id || it.parentId == id) {
                    it.copy(
                        isDeleted = true,
                        isPending = true,
                    )
                } else {
                    it
                }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        uploadedUserIds += userId
        return pushError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}

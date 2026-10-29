package com.quare.blitzsplit.core.groups.testing

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory GroupRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeGroupRepository(
    initialGroups: List<Group> = emptyList(),
) : GroupRepository {
    val groups = MutableStateFlow(initialGroups)
    val hasPendingChanges = MutableStateFlow(false)
    val createdGroups = mutableListOf<Group>()
    val createdAts = mutableListOf<Long>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var createError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearError: Throwable? = null
    var clearCount = 0

    override fun observeGroups(): Flow<List<Group>> = groups

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun createGroup(
        group: Group,
        createdAt: Long,
    ): Result<Unit> {
        createError?.let { return Result.failure(it) }
        createdGroups += group
        createdAts += createdAt
        groups.value = listOf(group) + groups.value
        hasPendingChanges.value = true
        return Result.success(Unit)
    }

    override suspend fun uploadPendingGroups(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun downloadGroups(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        clearError?.let { throw it }
        groups.value = emptyList()
        hasPendingChanges.value = false
    }
}

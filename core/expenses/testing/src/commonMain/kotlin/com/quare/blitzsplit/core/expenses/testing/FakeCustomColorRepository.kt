package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory CustomColorRepository: the palette and what waits to go up are public, and the failure knobs are public vars.
class FakeCustomColorRepository(
    initialColors: List<CustomColor> = emptyList(),
) : CustomColorRepository {
    val colors = MutableStateFlow(initialColors)
    val pending = MutableStateFlow<Set<CustomColor>>(emptySet())
    val addedAt = mutableListOf<Pair<CustomColor, Long>>()
    val deletedAt = mutableListOf<Pair<CustomColor, Long>>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var isCleared = false
    var saveError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearError: Throwable? = null

    override fun observeColors(): Flow<List<CustomColor>> = colors

    override fun observeHasPendingChanges(): Flow<Boolean> = pending.map { it.isNotEmpty() }

    override fun observeVersions(): Flow<List<Long>> = colors.map { list -> list.map { it.rgb.toLong() } }

    override suspend fun add(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        addedAt += color to updatedAt
        colors.update { if (color in it) it else it + color }
        pending.update { it + color }
        return Result.success(Unit)
    }

    override suspend fun delete(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        deletedAt += color to updatedAt
        colors.update { it - color }
        pending.update { it + color }
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        pushError?.let { return Result.failure(it) }
        uploadedUserIds += userId
        pending.value = emptySet()
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        pullError?.let { return Result.failure(it) }
        downloadedUserIds += userId
        return Result.success(Unit)
    }

    override suspend fun clear() {
        clearError?.let { throw it }
        isCleared = true
        colors.value = emptyList()
        pending.value = emptySet()
    }
}

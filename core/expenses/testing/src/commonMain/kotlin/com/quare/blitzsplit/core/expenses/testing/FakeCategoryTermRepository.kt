package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory CategoryTermRepository: the changes are public so a test can set and read them, and the failure knobs are public vars.
class FakeCategoryTermRepository(
    initialChanges: List<CategoryTermChange> = emptyList(),
) : CategoryTermRepository {
    val changes = MutableStateFlow(initialChanges)
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var isCleared = false
    var saveError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeChanges(): Flow<List<CategoryTermChange>> = changes

    override fun observeHasPendingChanges(): Flow<Boolean> = changes.map { list -> list.any { it.isPending } }

    override suspend fun saveTerm(
        term: String,
        categoryId: String?,
        updatedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        val key = CategoryMatcher.keyOf(term)
        val change = CategoryTermChange(
            key = key,
            term = term.trim(),
            categoryId = categoryId,
            isRestored = false,
            isPending = true,
        )
        changes.update { list -> list.filterNot { it.key == key } + change }
        return Result.success(Unit)
    }

    override suspend fun restoreAppTerms(
        appKeys: Set<String>,
        updatedAt: Long,
    ): Result<Unit> {
        saveError?.let { return Result.failure(it) }
        changes.update { list ->
            list.map { if (it.key in appKeys) it.copy(isRestored = true, isPending = true) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        pushError?.let { return Result.failure(it) }
        uploadedUserIds += userId
        changes.update { list -> list.map { it.copy(isPending = false) } }
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        pullError?.let { return Result.failure(it) }
        downloadedUserIds += userId
        return Result.success(Unit)
    }

    override suspend fun clear() {
        isCleared = true
        changes.value = emptyList()
    }
}

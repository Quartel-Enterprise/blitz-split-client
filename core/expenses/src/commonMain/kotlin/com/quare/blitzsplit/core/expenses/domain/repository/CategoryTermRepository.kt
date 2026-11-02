package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import kotlinx.coroutines.flow.Flow

// Why: the person's category map (flow 9), on the device and offline first, synced to their other devices.
interface CategoryTermRepository {
    fun observeChanges(): Flow<List<CategoryTermChange>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: a term added, moved or, with a null category, removed.
    suspend fun saveTerm(
        term: String,
        categoryId: String?,
        updatedAt: Long,
    ): Result<Unit>

    // Why: every change to the app's terms stops counting; the person's own terms stay.
    suspend fun restoreAppTerms(
        appKeys: Set<String>,
        updatedAt: Long,
    ): Result<Unit>

    suspend fun uploadPending(userId: String): Result<Unit>

    suspend fun download(userId: String): Result<Unit>

    suspend fun clear()
}

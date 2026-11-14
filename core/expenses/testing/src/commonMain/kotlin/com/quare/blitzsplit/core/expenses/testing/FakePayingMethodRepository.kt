package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// In-memory PayingMethodRepository: the methods and marks are public so a test can set and read them, and the failure knobs are public vars.
class FakePayingMethodRepository(
    initialMethods: List<PayingMethod> = emptyList(),
    initialMarks: Map<String, String> = emptyMap(),
) : PayingMethodRepository {
    val methods = MutableStateFlow(initialMethods)
    val marks = MutableStateFlow(initialMarks)
    val hasPendingChanges = MutableStateFlow(false)
    val syncedUserIds = mutableListOf<Pair<String, Boolean>>()
    var isCleared = false
    var saveError: Throwable? = null
    var syncError: Throwable? = null

    override fun observeMethods(): Flow<List<PayingMethod>> = methods

    override fun observeMarks(): Flow<Map<String, String>> = marks

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun saveMethods(methods: List<PayingMethod>) {
        saveError?.let { throw it }
        val saved = methods.associateBy { it.id }
        this.methods.update { list -> list.map { saved[it.id] ?: it } + methods.filter { new -> list.none { it.id == new.id } } }
    }

    override suspend fun deleteMethod(id: String) {
        saveError?.let { throw it }
        methods.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markBill(
        expenseId: String,
        methodId: String?,
    ) {
        saveError?.let { throw it }
        marks.update { if (methodId == null) it - expenseId else it + (expenseId to methodId) }
    }

    override suspend fun sync(
        userId: String,
        withMarks: Boolean,
    ): Result<Unit> {
        syncError?.let { return Result.failure(it) }
        syncedUserIds += userId to withMarks
        return Result.success(Unit)
    }

    override suspend fun clear() {
        isCleared = true
        methods.value = emptyList()
        marks.value = emptyMap()
    }
}

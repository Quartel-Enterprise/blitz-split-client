package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import kotlinx.coroutines.flow.Flow

// Why: the person's payment methods and the bills they marked with them, on the device and offline first, synced only to their own devices.
interface PayingMethodRepository {
    // Why: in the order they were added, the default first.
    fun observeMethods(): Flow<List<PayingMethod>>

    // Why: each marked bill's method id, by the bill's id.
    fun observeMarks(): Flow<Map<String, String>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: one write for a method and the default it takes over from, so no one sees two defaults.
    suspend fun saveMethods(methods: List<PayingMethod>)

    suspend fun deleteMethod(id: String)

    // Why: a null method clears the bill's mark.
    suspend fun markBill(
        expenseId: String,
        methodId: String?,
    )

    // Why: the methods, then, once the bills are on the server (withMarks), the marks, which need them there.
    suspend fun sync(
        userId: String,
        withMarks: Boolean,
    ): Result<Unit>

    suspend fun clear()
}

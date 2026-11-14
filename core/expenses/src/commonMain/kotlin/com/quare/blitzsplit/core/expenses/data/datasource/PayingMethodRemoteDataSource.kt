package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto

interface PayingMethodRemoteDataSource {
    suspend fun upsertMethods(methods: List<PayingMethodDto>)

    // Why: tombstones included, so a method deleted on another device goes away here too.
    suspend fun fetchMethods(userId: String): List<PayingMethodDto>

    suspend fun upsertMarks(marks: List<BillPayingMethodDto>)

    suspend fun fetchMarks(userId: String): List<BillPayingMethodDto>
}

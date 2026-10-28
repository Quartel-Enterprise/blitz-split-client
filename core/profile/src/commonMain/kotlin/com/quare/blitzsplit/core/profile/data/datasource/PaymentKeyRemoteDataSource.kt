package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto

interface PaymentKeyRemoteDataSource {
    suspend fun upsert(keys: List<PaymentKeyDto>)

    // Why: tombstones included, so a key deleted on another device goes away here too.
    suspend fun fetch(userId: String): List<PaymentKeyDto>
}

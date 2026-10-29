package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabasePaymentKeyRemoteDataSource(
    private val postgrest: Postgrest,
) : PaymentKeyRemoteDataSource {
    override suspend fun upsert(keys: List<PaymentKeyDto>) {
        postgrest.from(TABLE).upsert(keys)
    }

    override suspend fun fetch(userId: String): List<PaymentKeyDto> = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<PaymentKeyDto>()

    private companion object {
        const val TABLE = "payment_keys"
    }
}

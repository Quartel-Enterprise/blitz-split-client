package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto

interface PaymentKeyRemoteDataSource {
    suspend fun upsert(keys: List<PaymentKeyDto>)

    // Why: tombstones included, so a key deleted on another device goes away here too.
    suspend fun fetch(userId: String): List<PaymentKeyDto>

    /*
     * Why: the live keys of everyone else the server lets this user read, the people they share a group with
     * (`shares_group_with`), for "Pagar Ana" (4.5). The server never sends their tombstones.
     */
    suspend fun fetchOthers(userId: String): List<PaymentKeyDto>
}

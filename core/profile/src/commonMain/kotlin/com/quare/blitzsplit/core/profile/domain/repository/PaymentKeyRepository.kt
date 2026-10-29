package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.model.PaymentKey
import kotlinx.coroutines.flow.Flow

interface PaymentKeyRepository {
    // Why: Pix first, each method's default first and then in the order the keys were added.
    fun observeKeys(): Flow<List<PaymentKey>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: one write for a key and the default it takes over from, so no one sees two defaults or none.
    suspend fun saveKeys(keys: List<PaymentKey>)

    // Why: in the same write as the tombstone, the key that takes over as default, if the deleted one was it.
    suspend fun deleteKey(
        id: String,
        nextDefault: PaymentKey?,
    )

    suspend fun uploadPendingKeys(userId: String): Result<Unit>

    // Why: after a switch, another user's keys give way to this user's, even ones not pushed yet.
    suspend fun downloadKeys(userId: String): Result<Unit>

    // Why: a sign out leaves no key on the device.
    suspend fun clear()
}

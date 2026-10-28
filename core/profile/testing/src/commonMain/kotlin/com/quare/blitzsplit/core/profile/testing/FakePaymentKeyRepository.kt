package com.quare.blitzsplit.core.profile.testing

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory PaymentKeyRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakePaymentKeyRepository(
    initialKeys: List<PaymentKey> = emptyList(),
) : PaymentKeyRepository {
    val keys = MutableStateFlow(initialKeys)
    val hasPendingChanges = MutableStateFlow(false)
    val savedKeys = mutableListOf<List<PaymentKey>>()
    val deletedIds = mutableListOf<String>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearCount = 0

    override fun observeKeys(): Flow<List<PaymentKey>> = keys

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun saveKeys(keys: List<PaymentKey>) {
        savedKeys += keys
        val savedIds = keys.map { it.id }.toSet()
        this.keys.value = this.keys.value.filterNot { it.id in savedIds } + keys
        hasPendingChanges.value = true
    }

    override suspend fun deleteKey(id: String) {
        deletedIds += id
        keys.value = keys.value.filterNot { it.id == id }
        hasPendingChanges.value = true
    }

    override suspend fun uploadPendingKeys(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun downloadKeys(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        keys.value = emptyList()
        hasPendingChanges.value = false
    }
}

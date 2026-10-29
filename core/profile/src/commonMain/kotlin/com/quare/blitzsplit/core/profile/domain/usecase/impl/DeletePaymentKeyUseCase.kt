package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.DeletePaymentKey
import kotlinx.coroutines.flow.first

// Why: deleting the default hands it to the oldest key left of that method, so the method keeps one.
class DeletePaymentKeyUseCase(
    private val repository: PaymentKeyRepository,
) : DeletePaymentKey {
    override suspend fun invoke(keyId: String) {
        val keys = repository.observeKeys().first()
        val key = keys.find { it.id == keyId } ?: return
        val nextDefault = keys
            .filter { it.type.method == key.type.method && it.id != keyId }
            .minByOrNull { it.addedAt }
            ?.takeIf { key.isDefault }
        repository.deleteKey(
            id = keyId,
            nextDefault = nextDefault,
        )
    }
}

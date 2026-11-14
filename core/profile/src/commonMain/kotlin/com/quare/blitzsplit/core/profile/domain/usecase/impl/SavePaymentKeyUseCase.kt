package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.usecase.SavePaymentKey
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

/*
 * Why: each method always has one default. The first key of a method is it, a key made default takes over from
 * the old one, and the default can't be turned off: another key has to take its place. One CPF/CNPJ, phone and email
 * key per person (PaymentKeyType.isOnePerPerson), whatever the caller, so a key of a held type saves over it.
 */
class SavePaymentKeyUseCase(
    private val repository: PaymentKeyRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SavePaymentKey {
    override suspend fun invoke(
        keyId: String?,
        type: PaymentKeyType,
        value: String,
        isDefault: Boolean,
    ) {
        val keys = repository.observeKeys().first()
        // Why: a new CPF/CNPJ, phone or email key replaces the one of that type already held, never adding a second.
        val existing = keyId?.let { id -> keys.find { it.id == id } }
            ?: keys.find { it.type == type && type.isOnePerPerson }.takeIf { keyId == null }
        val others = keys.filter { it.type.method == type.method && it.id != existing?.id }
        val isNewDefault = isDefault || existing?.isDefault == true || others.isEmpty()
        val key = PaymentKey(
            id = existing?.id ?: Uuid.random().toString(),
            type = type,
            value = value,
            isDefault = isNewDefault,
            addedAt = existing?.addedAt ?: currentTimeMillis(),
        )
        val demoted = if (isNewDefault) {
            others.filter { it.isDefault }.map {
                it.copy(
                    isDefault = false,
                )
            }
        } else {
            emptyList()
        }
        repository.saveKeys(listOf(key) + demoted)
    }
}

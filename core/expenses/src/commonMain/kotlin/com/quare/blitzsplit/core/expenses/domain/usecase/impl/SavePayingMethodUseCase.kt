package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodRules
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SavePayingMethod
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

class SavePayingMethodUseCase(
    private val repository: PayingMethodRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SavePayingMethod {
    override suspend fun invoke(
        methodId: String?,
        type: PayingMethodType,
        name: String,
        cardColor: CardColor?,
        isDefault: Boolean,
    ): Result<Unit> = suspendRunCatching {
        require(PayingMethodRules.isNameValid(type, name)) { "The name doesn't fit the type" }
        val methods = repository.observeMethods().first()
        val existing = methodId?.let { id -> methods.find { it.id == id } }
        // Why: a method deleted meanwhile (on another device) is gone, never saved again under a new id.
        require(methodId == null || existing != null) { "The method was deleted" }
        val others = methods.filterNot { it.id == existing?.id }
        require(!type.isOnePerPerson || others.none { it.type == type }) { "Only one method of this type" }
        val method = PayingMethod(
            id = existing?.id ?: Uuid.random().toString(),
            type = type,
            name = name.trim(),
            cardColor = cardColor.takeIf { type.isCard },
            isDefault = isDefault,
            addedAt = existing?.addedAt ?: currentTimeMillis(),
        )
        val formerDefaults = if (isDefault) {
            others.filter { it.isDefault }.map { it.copy(isDefault = false) }
        } else {
            emptyList()
        }
        repository.saveMethods(formerDefaults + method)
    }
}

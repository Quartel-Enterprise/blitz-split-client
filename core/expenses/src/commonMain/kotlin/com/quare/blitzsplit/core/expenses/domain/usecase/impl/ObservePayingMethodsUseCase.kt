package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePayingMethods
import kotlinx.coroutines.flow.Flow

class ObservePayingMethodsUseCase(
    private val repository: PayingMethodRepository,
) : ObservePayingMethods {
    override fun invoke(): Flow<List<PayingMethod>> = repository.observeMethods()
}

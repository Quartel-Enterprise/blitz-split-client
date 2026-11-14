package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteCustomColor
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class DeleteCustomColorUseCase(
    private val repository: CustomColorRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : DeleteCustomColor {
    override suspend fun invoke(color: CustomColor): Result<Unit> = repository.delete(color, currentTimeMillis())
}

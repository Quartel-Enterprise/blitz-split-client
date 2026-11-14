package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCustomColors
import kotlinx.coroutines.flow.Flow

class ObserveCustomColorsUseCase(
    private val repository: CustomColorRepository,
) : ObserveCustomColors {
    override fun invoke(): Flow<List<CustomColor>> = repository.observeColors()
}

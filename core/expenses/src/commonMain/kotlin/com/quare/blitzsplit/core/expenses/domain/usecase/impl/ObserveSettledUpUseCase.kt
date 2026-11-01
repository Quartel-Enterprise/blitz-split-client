package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.repository.SettledUpRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import kotlinx.coroutines.flow.Flow

class ObserveSettledUpUseCase(
    private val repository: SettledUpRepository,
) : ObserveSettledUp {
    override fun invoke(): Flow<SettledUp?> = repository.observeSettledUp()
}

package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.repository.SettledUpRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeSettledUp

class ConsumeSettledUpUseCase(
    private val repository: SettledUpRepository,
) : ConsumeSettledUp {
    override fun invoke(settledUp: SettledUp) {
        repository.consume(settledUp)
    }
}

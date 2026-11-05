package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.AddPersonRequestRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ConsumeAddPersonRequest

class ConsumeAddPersonRequestUseCase(
    private val repository: AddPersonRequestRepository,
) : ConsumeAddPersonRequest {
    override fun invoke(groupId: String) {
        repository.consume(groupId)
    }
}

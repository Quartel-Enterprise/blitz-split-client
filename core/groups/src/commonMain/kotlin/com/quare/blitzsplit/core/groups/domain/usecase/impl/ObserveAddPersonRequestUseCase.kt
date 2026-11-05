package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.AddPersonRequestRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveAddPersonRequest
import kotlinx.coroutines.flow.Flow

class ObserveAddPersonRequestUseCase(
    private val repository: AddPersonRequestRepository,
) : ObserveAddPersonRequest {
    override fun invoke(): Flow<String?> = repository.observeRequest()
}

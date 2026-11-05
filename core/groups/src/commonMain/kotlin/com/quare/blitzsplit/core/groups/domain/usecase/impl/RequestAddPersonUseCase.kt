package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.AddPersonRequestRepository
import com.quare.blitzsplit.core.groups.domain.usecase.RequestAddPerson

class RequestAddPersonUseCase(
    private val repository: AddPersonRequestRepository,
) : RequestAddPerson {
    override fun invoke(groupId: String) {
        repository.post(groupId)
    }
}

package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveMemberShareCode
import kotlinx.coroutines.flow.Flow

class ObserveMemberShareCodeUseCase(
    private val repository: GroupRepository,
) : ObserveMemberShareCode {
    override fun invoke(userId: String): Flow<String?> = repository.observeMemberShareCode(userId)
}

package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.usecase.StartUpdate

class StartUpdateUseCase(
    private val repository: AppUpdateRepository,
) : StartUpdate {
    override suspend fun invoke() {
        repository.startUpdate()
    }
}

package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate

class CheckForUpdateUseCase(
    private val repository: AppUpdateRepository,
) : CheckForUpdate {
    override suspend fun invoke(): UpdateAvailability = repository.check()
}

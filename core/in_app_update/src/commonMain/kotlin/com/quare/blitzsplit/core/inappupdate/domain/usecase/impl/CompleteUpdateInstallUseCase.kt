package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CompleteUpdateInstall

class CompleteUpdateInstallUseCase(
    private val repository: AppUpdateRepository,
) : CompleteUpdateInstall {
    override suspend fun invoke() {
        repository.completeInstall()
    }
}

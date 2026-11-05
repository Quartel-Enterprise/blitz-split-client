package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite

class DismissAppInviteUseCase(
    private val repository: AppInviteRepository,
) : DismissAppInvite {
    override suspend fun invoke() {
        repository.dismiss()
    }
}

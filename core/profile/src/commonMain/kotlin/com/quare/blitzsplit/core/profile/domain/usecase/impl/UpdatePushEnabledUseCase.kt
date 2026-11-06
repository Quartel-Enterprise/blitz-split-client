package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.profile.domain.usecase.UpdatePushEnabled

class UpdatePushEnabledUseCase(
    private val repository: PushDeviceRepository,
) : UpdatePushEnabled {
    override suspend fun invoke(isEnabled: Boolean) {
        repository.setEnabled(isEnabled)
    }
}

package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePushEnabled
import kotlinx.coroutines.flow.Flow

class ObservePushEnabledUseCase(
    private val repository: PushDeviceRepository,
) : ObservePushEnabled {
    override fun invoke(): Flow<Boolean> = repository.observeIsEnabled()
}

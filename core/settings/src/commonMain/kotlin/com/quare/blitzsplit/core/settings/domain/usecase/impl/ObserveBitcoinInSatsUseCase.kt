package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import kotlinx.coroutines.flow.Flow

class ObserveBitcoinInSatsUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveBitcoinInSats {
    override fun invoke(): Flow<Boolean> = repository.observeBitcoinInSats()
}

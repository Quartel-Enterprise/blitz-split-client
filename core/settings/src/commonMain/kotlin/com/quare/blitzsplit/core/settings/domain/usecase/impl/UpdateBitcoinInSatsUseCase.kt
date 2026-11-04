package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateBitcoinInSats

class UpdateBitcoinInSatsUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateBitcoinInSats {
    override suspend fun invoke(isOn: Boolean) {
        repository.updateBitcoinInSats(isOn)
    }
}

package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppBannerDismissed
import kotlinx.coroutines.flow.Flow

class ObserveAppBannerDismissedUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveAppBannerDismissed {
    override fun invoke(): Flow<Boolean> = repository.observeAppBannerDismissed()
}

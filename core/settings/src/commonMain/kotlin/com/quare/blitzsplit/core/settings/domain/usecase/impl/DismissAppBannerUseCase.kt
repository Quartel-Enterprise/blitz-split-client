package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.DismissAppBanner

class DismissAppBannerUseCase(
    private val repository: DeviceSettingsRepository,
) : DismissAppBanner {
    override suspend fun invoke() {
        repository.dismissAppBanner()
    }
}

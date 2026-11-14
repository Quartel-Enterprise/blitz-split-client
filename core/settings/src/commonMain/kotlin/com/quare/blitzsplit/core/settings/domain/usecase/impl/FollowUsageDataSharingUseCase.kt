package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.FollowUsageDataSharing

class FollowUsageDataSharingUseCase(
    private val repository: DeviceSettingsRepository,
    private val hub: AnalyticsHub,
) : FollowUsageDataSharing {
    override suspend fun invoke() {
        repository.observeUsageDataSharing().collect(hub::setCollectionEnabled)
    }
}

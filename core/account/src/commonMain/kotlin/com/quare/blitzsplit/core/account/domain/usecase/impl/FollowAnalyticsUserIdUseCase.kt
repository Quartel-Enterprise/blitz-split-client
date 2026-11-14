package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.FollowAnalyticsUserId
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FollowAnalyticsUserIdUseCase(
    private val repository: AccountRepository,
    private val hub: AnalyticsHub,
) : FollowAnalyticsUserId {
    override suspend fun invoke() {
        repository
            .observeStatus()
            .map(::toUserId)
            .distinctUntilChanged()
            .collect(hub::setUserId)
    }

    private fun toUserId(status: AccountStatus): String? = when (status) {
        AccountStatus.LocalOnly -> null
        is AccountStatus.Anonymous -> status.userId
        is AccountStatus.Connected -> status.userId
    }
}

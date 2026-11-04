package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.AppForeground
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveAppForeground
import kotlinx.coroutines.flow.Flow

class ObserveAppForegroundUseCase(
    private val appForeground: AppForeground,
) : ObserveAppForeground {
    override fun invoke(): Flow<Boolean> = appForeground.observe()
}

package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.AppForeground
import com.quare.blitzsplit.core.sync.domain.usecase.ReportAppForeground

class ReportAppForegroundUseCase(
    private val appForeground: AppForeground,
) : ReportAppForeground {
    override fun invoke(isForeground: Boolean) {
        appForeground.update(isForeground)
    }
}

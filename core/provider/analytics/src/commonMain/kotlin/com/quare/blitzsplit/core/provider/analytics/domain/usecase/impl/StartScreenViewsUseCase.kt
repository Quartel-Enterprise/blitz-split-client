package com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.StartScreenViews
import com.quare.blitzsplit.core.provider.analytics.screen.ScreenViewTracker

internal class StartScreenViewsUseCase(
    private val tracker: ScreenViewTracker,
) : StartScreenViews {
    override fun invoke() {
        tracker.start()
    }
}

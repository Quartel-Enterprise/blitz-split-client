package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveAppBannerDismissed {
    operator fun invoke(): Flow<Boolean>
}

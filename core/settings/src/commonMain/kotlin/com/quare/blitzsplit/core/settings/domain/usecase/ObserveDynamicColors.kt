package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveDynamicColors {
    operator fun invoke(): Flow<Boolean>
}

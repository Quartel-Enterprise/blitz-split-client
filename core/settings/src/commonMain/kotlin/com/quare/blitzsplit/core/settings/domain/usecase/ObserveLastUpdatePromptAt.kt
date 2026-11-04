package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveLastUpdatePromptAt {
    operator fun invoke(): Flow<Long?>
}

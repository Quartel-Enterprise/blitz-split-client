package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveLastNotificationPromptAt {
    operator fun invoke(): Flow<Long?>
}

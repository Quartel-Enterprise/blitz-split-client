package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateLastNotificationPromptAt {
    suspend operator fun invoke(epochMillis: Long)
}

package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateLastUpdatePromptAt {
    suspend operator fun invoke(epochMillis: Long)
}

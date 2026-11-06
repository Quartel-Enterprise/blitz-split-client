package com.quare.blitzsplit.core.profile.domain.usecase

fun interface UpdatePushEnabled {
    suspend operator fun invoke(isEnabled: Boolean)
}

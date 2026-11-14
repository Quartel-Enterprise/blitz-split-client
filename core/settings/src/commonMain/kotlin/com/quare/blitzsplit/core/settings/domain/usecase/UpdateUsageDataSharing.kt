package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateUsageDataSharing {
    suspend operator fun invoke(isOn: Boolean)
}

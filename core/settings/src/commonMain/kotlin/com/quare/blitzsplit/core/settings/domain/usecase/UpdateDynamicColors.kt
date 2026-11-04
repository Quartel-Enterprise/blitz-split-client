package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateDynamicColors {
    suspend operator fun invoke(isOn: Boolean)
}

package com.quare.blitzsplit.core.settings.domain.usecase

fun interface UpdateBetaReleaseNotesChoice {
    suspend operator fun invoke(isOn: Boolean)
}

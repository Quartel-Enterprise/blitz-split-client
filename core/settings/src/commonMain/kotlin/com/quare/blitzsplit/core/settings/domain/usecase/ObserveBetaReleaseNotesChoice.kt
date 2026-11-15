package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: Release notes' "Show beta versions" as switched on this device, null until it was.
fun interface ObserveBetaReleaseNotesChoice {
    operator fun invoke(): Flow<Boolean?>
}

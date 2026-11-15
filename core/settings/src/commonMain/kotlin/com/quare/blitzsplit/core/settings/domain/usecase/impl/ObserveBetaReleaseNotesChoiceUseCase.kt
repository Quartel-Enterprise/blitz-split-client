package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBetaReleaseNotesChoice
import kotlinx.coroutines.flow.Flow

class ObserveBetaReleaseNotesChoiceUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveBetaReleaseNotesChoice {
    override fun invoke(): Flow<Boolean?> = repository.observeBetaReleaseNotesChoice()
}

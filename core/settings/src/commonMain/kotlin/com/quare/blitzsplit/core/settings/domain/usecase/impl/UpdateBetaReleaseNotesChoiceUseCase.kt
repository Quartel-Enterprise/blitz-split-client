package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateBetaReleaseNotesChoice

class UpdateBetaReleaseNotesChoiceUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateBetaReleaseNotesChoice {
    override suspend fun invoke(isOn: Boolean) {
        repository.updateBetaReleaseNotesChoice(isOn)
    }
}

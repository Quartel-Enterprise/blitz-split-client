package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ObserveDynamicColorsUseCase(
    private val repository: DeviceSettingsRepository,
    private val dynamicColorSupport: DynamicColorSupport,
) : ObserveDynamicColors {
    // Why: a choice restored onto a device without Material You (a backup on Android 11) keeps the brand's colors.
    override fun invoke(): Flow<Boolean> =
        if (dynamicColorSupport()) repository.observeDynamicColors() else flowOf(false)
}

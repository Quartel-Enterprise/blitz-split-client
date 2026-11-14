package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import com.quare.blitzsplit.core.utils.DeviceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveSpokenLanguageUseCase(
    private val observeAppLanguage: ObserveAppLanguage,
    private val deviceLanguage: DeviceLanguage,
) : ObserveSpokenLanguage {
    override fun invoke(): Flow<String> = observeAppLanguage().map { picked ->
        // Why: following the system, the app speaks the device's language only when it has it.
        val spoken = picked.takeIf { it != AppLanguage.SYSTEM } ?: AppLanguage.fromTag(deviceLanguage())
        AppLanguage.languageOf(spoken.tag) ?: DEFAULT_LANGUAGE
    }.distinctUntilChanged()

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

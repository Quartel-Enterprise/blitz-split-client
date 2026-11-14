package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import com.quare.blitzsplit.core.utils.DeviceLanguages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveSpokenLanguageUseCase(
    private val observeAppLanguage: ObserveAppLanguage,
    private val deviceLanguages: DeviceLanguages,
) : ObserveSpokenLanguage {
    override fun invoke(): Flow<String> = observeAppLanguage()
        .map { picked ->
            // Why: following the system, the app speaks the first of the device's languages it has, as each platform picks.
            val spoken = picked.takeIf { it != AppLanguage.SYSTEM }
                ?: deviceLanguages().map(AppLanguage::fromTag).firstOrNull { it != AppLanguage.SYSTEM }
                ?: AppLanguage.SYSTEM
            AppLanguage.languageOf(spoken.tag) ?: DEFAULT_LANGUAGE
        }.distinctUntilChanged()

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

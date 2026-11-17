package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.utils.DeviceLanguage
import kotlinx.coroutines.flow.first

class GetLegalLanguageUseCase(
    private val observeAppLanguage: ObserveAppLanguage,
    private val deviceLanguage: DeviceLanguage,
) : GetLegalLanguage {
    // Why: it goes into the file's address, so nothing but a language's own letters, or Traditional Chinese's "zh-Hant".
    private val languagePattern = Regex("[a-z]{2,3}(-Hant)?")

    override suspend fun invoke(): String {
        val tag = observeAppLanguage().first().tag ?: deviceLanguage()
        // Why: a Traditional Chinese tag reads Taiwan's pages, "zh-Hant", the bare "zh" being Simplified's.
        val language = AppLanguage
            .fromTag(tag)
            .takeIf { it == AppLanguage.CHINESE_TRADITIONAL }
            ?.spokenCode
            ?: AppLanguage.languageOf(tag)
        return language?.takeIf { languagePattern.matches(it) } ?: DEFAULT_LANGUAGE
    }

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

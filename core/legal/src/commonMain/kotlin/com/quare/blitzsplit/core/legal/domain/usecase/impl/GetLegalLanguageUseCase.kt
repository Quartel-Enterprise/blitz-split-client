package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.utils.DeviceLanguage
import kotlinx.coroutines.flow.first

class GetLegalLanguageUseCase(
    private val observeAppLanguage: ObserveAppLanguage,
    private val deviceLanguage: DeviceLanguage,
) : GetLegalLanguage {
    // Why: it goes into the file's address, so nothing but a language's own letters.
    private val languagePattern = Regex("[a-z]{2,3}")

    override suspend fun invoke(): String {
        val tag = observeAppLanguage().first().tag ?: deviceLanguage()
        return tag
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.lowercase()
            ?.takeIf { languagePattern.matches(it) }
            // Why: an older Android still names Indonesian by the code ISO retired.
            ?.let { if (it == LEGACY_INDONESIAN) INDONESIAN else it }
            ?: DEFAULT_LANGUAGE
    }

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
        const val LEGACY_INDONESIAN = "in"
        const val INDONESIAN = "id"
    }
}

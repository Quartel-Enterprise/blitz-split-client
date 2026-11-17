package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetReleaseNotesLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import kotlinx.coroutines.flow.first

class GetReleaseNotesLanguageUseCase(
    private val observeSpokenLanguage: ObserveSpokenLanguage,
) : GetReleaseNotesLanguage {
    // Why: it goes into the file's address, so nothing but a language's own letters, or Traditional Chinese's "zh-Hant".
    private val languagePattern = Regex("[a-z]{2,3}(-Hant)?")

    override suspend fun invoke(): String =
        observeSpokenLanguage().first().takeIf { languagePattern.matches(it) } ?: DEFAULT_LANGUAGE

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

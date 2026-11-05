package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore

internal class FakeAppLanguageStore(
    // Why: what the platform keeps; a test sets it directly to change the language outside the app.
    var stored: AppLanguage = AppLanguage.SYSTEM,
) : AppLanguageStore {
    var readCount = 0
        private set
    val writes = mutableListOf<AppLanguage>()

    override fun read(): AppLanguage {
        readCount++
        return stored
    }

    override fun write(language: AppLanguage) {
        writes += language
        stored = language
    }
}

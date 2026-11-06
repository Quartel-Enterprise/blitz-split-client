package com.quare.blitzsplit.shared.settings

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.datasource.AppLanguageLocalDataSource

// Why: the jvm target only runs the tests, which have no per-app language to keep.
internal class InMemoryAppLanguageLocalDataSource : AppLanguageLocalDataSource {
    private var language = AppLanguage.SYSTEM

    override fun read(): AppLanguage = language

    override fun write(language: AppLanguage) {
        this.language = language
    }
}

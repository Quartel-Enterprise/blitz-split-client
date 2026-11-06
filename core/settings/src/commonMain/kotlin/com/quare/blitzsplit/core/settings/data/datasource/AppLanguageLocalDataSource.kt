package com.quare.blitzsplit.core.settings.data.datasource

import com.quare.blitzsplit.core.model.AppLanguage

/*
 * Why: the per-app language each platform keeps itself (Android's LocaleManager, Apple's AppleLanguages, the browser's
 * storage), since it applies the language before any screen draws and its own settings may change it too.
 */
interface AppLanguageLocalDataSource {
    fun read(): AppLanguage

    fun write(language: AppLanguage)
}

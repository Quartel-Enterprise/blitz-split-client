package com.quare.blitzsplit.shared.settings

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore
import kotlinx.browser.localStorage

/*
 * Why: the browser's storage, which webApp/src/i18n reads under the same key before the page draws; the page reloads
 * in the language picked. Without storage the page follows the browser's languages.
 */
internal class WebAppLanguageStore : AppLanguageStore {
    override fun read(): AppLanguage = try {
        AppLanguage.fromTag(localStorage.getItem(STORAGE_KEY))
    } catch (unavailable: Throwable) {
        AppLanguage.SYSTEM
    }

    override fun write(language: AppLanguage) {
        try {
            val tag = language.tag
            if (tag == null) localStorage.removeItem(STORAGE_KEY) else localStorage.setItem(STORAGE_KEY, tag)
        } catch (unavailable: Throwable) {
            // Why: nothing to keep it in, so the page stays in the browser's language.
        }
    }

    private companion object {
        const val STORAGE_KEY = "language"
    }
}

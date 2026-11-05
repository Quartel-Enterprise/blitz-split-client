package com.quare.blitzsplit.shared.settings

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/*
 * Why: the app's own AppleLanguages, which the Settings app's per-app language writes too and the system reads at
 * launch; the SwiftUI root redraws in the one picked at once.
 */
internal class AppleAppLanguageStore : AppLanguageStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    // Why: only the app's own domain, since the search list falls back to the device's languages.
    override fun read(): AppLanguage {
        val domain = NSBundle.mainBundle.bundleIdentifier?.let(defaults::persistentDomainForName)
        val languages = domain?.get(APPLE_LANGUAGES) as? List<*>
        return AppLanguage.fromTag(languages?.firstOrNull() as? String)
    }

    override fun write(language: AppLanguage) {
        val tag = language.tag
        if (tag ==
            null
        ) {
            defaults.removeObjectForKey(APPLE_LANGUAGES)
        } else {
            defaults.setObject(listOf(tag), APPLE_LANGUAGES)
        }
    }

    private companion object {
        const val APPLE_LANGUAGES = "AppleLanguages"
    }
}

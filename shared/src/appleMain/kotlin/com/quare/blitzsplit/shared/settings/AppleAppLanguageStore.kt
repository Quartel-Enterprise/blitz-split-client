package com.quare.blitzsplit.shared.settings

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

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

    // Why: the device's region, which AppleLanguages never changes ("ES" keeps Spain's Spanish).
    override fun write(language: AppLanguage) {
        val tag = language.toTag(NSLocale.currentLocale.countryCode)
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

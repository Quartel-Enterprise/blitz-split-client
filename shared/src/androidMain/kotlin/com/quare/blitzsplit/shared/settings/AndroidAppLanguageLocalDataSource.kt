package com.quare.blitzsplit.shared.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.datasource.AppLanguageLocalDataSource

/*
 * Why: Android 13 and later keep the per-app language themselves (also picked in the system's settings) and redraw the
 * app in it; before 13 the app keeps it and applies it to its activity.
 */
internal class AndroidAppLanguageLocalDataSource(
    private val context: Context,
) : AppLanguageLocalDataSource {
    override fun read(): AppLanguage = AppLanguage.fromTag(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context
                .getSystemService(LocaleManager::class.java)
                .applicationLocales
                .takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
        } else {
            LegacyAppLocale.getStoredTag(context)
        },
    )

    override fun write(language: AppLanguage) {
        val tag = language.toTag(getSystemRegion())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        } else {
            LegacyAppLocale.store(context, tag)
        }
    }

    // Why: the device's own region, which the app's language picked never changes ("ES" keeps Spain's Spanish).
    private fun getSystemRegion(): String? {
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).systemLocales
        } else {
            Resources.getSystem().configuration.locales
        }
        return locales
            .takeUnless { it.isEmpty }
            ?.get(0)
            ?.country
            ?.ifEmpty { null }
    }
}

package com.quare.blitzsplit.shared.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore

/*
 * Why: Android 13 and later keep the per-app language themselves (also picked in the system's settings) and redraw the
 * app in it; before 13 the app keeps it and applies it to its activity.
 */
internal class AndroidAppLanguageStore(
    private val context: Context,
) : AppLanguageStore {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        } else {
            LegacyAppLocale.store(context, language.tag)
        }
    }
}

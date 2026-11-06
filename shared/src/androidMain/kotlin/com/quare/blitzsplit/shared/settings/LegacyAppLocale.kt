package com.quare.blitzsplit.shared.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/*
 * Why: before Android 13 there is no per-app language, so the activity draws in the one Profile picked, kept here,
 * and opens again in it once picked; the system's settings don't list it there.
 */
object LegacyAppLocale {
    private const val PREFERENCES = "app_language"
    private const val KEY_TAG = "tag"

    // Why: MainActivity's attachBaseContext, so its first frame is already in the language picked.
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = getStoredTag(base)?.let(Locale::forLanguageTag)
        // Why: back to System, the formats follow the device again rather than the language picked before.
        Locale.setDefault(locale ?: Resources.getSystem().configuration.locales[0])
        if (locale == null) return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    internal fun getStoredTag(context: Context): String? = context
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getString(KEY_TAG, null)

    internal fun store(
        context: Context,
        tag: String?,
    ) {
        val editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
        if (tag == null) editor.remove(KEY_TAG) else editor.putString(KEY_TAG, tag)
        /*
         * Why: apply, off the main thread: the activity opening again right after reads it in attachBaseContext from
         * the same process's preferences, which already hold it.
         */
        editor.apply()
    }
}

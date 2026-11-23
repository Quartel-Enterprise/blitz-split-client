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

    private val latinDigitLanguages = setOf("ar", "bn", "ur", "ta")
    private const val NUMBERING_SYSTEM = "nu"
    private const val LATIN_DIGITS = "latn"

    // Why: MainActivity's attachBaseContext, so its first frame is already in the language picked.
    fun wrap(base: Context): Context = withLatinDigits(wrapLegacy(base))

    /*
     * Why: the application's own, without the Latin digits' configuration context, which would freeze its resources
     * in the language they started in: from Android 13 they follow a per-app language picked while the app runs.
     */
    fun wrapApplication(base: Context): Context = wrapLegacy(base)

    /*
     * Why: Arabic, Bengali, Urdu and Tamil write their numbers, amounts and dates with Latin digits (docs/product/general.md), so the
     * amount fields read what the number keyboard types. Every "%d" of a string resource, a date and a count follow the
     * context's locale, so an Arabic, Bengali, Urdu or Tamil one, picked or the device's, gets the "nu-latn" extension once here.
     */
    private fun withLatinDigits(context: Context): Context {
        val locale = context.resources.configuration.locales[0]
        val keepsItsDigits = locale.language !in latinDigitLanguages
        if (keepsItsDigits || locale.getUnicodeLocaleType(NUMBERING_SYSTEM) == LATIN_DIGITS) return context
        val latin = Locale
            .Builder()
            .setLocale(locale)
            .setUnicodeLocaleKeyword(NUMBERING_SYSTEM, LATIN_DIGITS)
            .build()
        Locale.setDefault(latin)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(latin)
        return context.createConfigurationContext(configuration)
    }

    private fun wrapLegacy(base: Context): Context {
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

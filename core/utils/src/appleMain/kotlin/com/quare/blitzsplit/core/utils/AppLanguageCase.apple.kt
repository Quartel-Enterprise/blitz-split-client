package com.quare.blitzsplit.core.utils

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.uppercaseStringWithLocale
import kotlin.concurrent.AtomicReference

private const val APPLE_LANGUAGES = "AppleLanguages"
private val pickedLocale = AtomicReference<Pair<String, NSLocale>?>(null)

/*
 * Why: Profile › Language puts the pick first in the app's own AppleLanguages; NSLocale reads it only at the next
 * launch. Only the app's domain, since the search list falls back to the device's languages, which may be one the app
 * doesn't draw in ("az" casing an English screen's "i" as "İ").
 */
@OptIn(BetaInteropApi::class)
actual fun String.toAppLanguageUppercase(): String {
    val defaults = NSUserDefaults.standardUserDefaults
    val domain = NSBundle.mainBundle.bundleIdentifier?.let(defaults::persistentDomainForName)
    val picked = (domain?.get(APPLE_LANGUAGES) as? List<*>)?.firstOrNull() as? String
    return NSString.create(string = this).uppercaseStringWithLocale(picked?.let(::localeOf) ?: NSLocale.currentLocale)
}

// Why: the locale built for the last language picked, so a list of names builds it once, not once per name.
private fun localeOf(identifier: String): NSLocale {
    pickedLocale.value?.takeIf { it.first == identifier }?.let { return it.second }
    return NSLocale(localeIdentifier = identifier).also { pickedLocale.value = identifier to it }
}

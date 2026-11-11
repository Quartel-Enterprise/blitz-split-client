package com.quare.blitzsplit.core.utils

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.uppercaseStringWithLocale

private const val APPLE_LANGUAGES = "AppleLanguages"

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
    val locale = picked?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
    return NSString.create(string = this).uppercaseStringWithLocale(locale)
}

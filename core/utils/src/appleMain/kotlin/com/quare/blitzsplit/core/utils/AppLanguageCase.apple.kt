package com.quare.blitzsplit.core.utils

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.uppercaseStringWithLocale

private const val APPLE_LANGUAGES = "AppleLanguages"

// Why: Profile › Language puts the pick first in the app's AppleLanguages; NSLocale reads it only at the next launch.
@OptIn(BetaInteropApi::class)
actual fun String.toAppLanguageUppercase(): String {
    val picked = NSUserDefaults.standardUserDefaults.stringArrayForKey(APPLE_LANGUAGES)?.firstOrNull() as? String
    val locale = picked?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
    return NSString.create(string = this).uppercaseStringWithLocale(locale)
}

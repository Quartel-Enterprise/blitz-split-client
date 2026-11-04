package com.quare.blitzsplit.core.inappupdate.data

/*
 * Why: the iPhone and Mac apps share their bundle id and listing (one purchase), so both read the same version; only
 * the page differs, which on the Mac opens in the Mac App Store rather than the browser.
 */
internal enum class AppStoreStorefront {
    IOS,
    MACOS,
    ;

    fun toStoreUrl(trackViewUrl: String): String = when (this) {
        IOS -> trackViewUrl
        MACOS -> trackViewUrl.replaceFirst("https://", "macappstore://")
    }
}

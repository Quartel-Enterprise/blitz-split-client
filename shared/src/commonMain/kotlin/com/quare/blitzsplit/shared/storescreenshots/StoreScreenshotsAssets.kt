package com.quare.blitzsplit.shared.storescreenshots

/*
 * Why: the photos the seed puts on the sample trip (faces/, groups/, receipt.jpg of scripts/store-screenshots/), which
 * each platform hands over its own way: the Apple apps from a folder they wrote them to, the web from the page.
 */
internal fun interface StoreScreenshotsAssets {
    // Why: a path as the seed names it (faces/person0.jpg, groups/<photo>.jpg, receipt.jpg), null when not handed over.
    fun read(path: String): ByteArray?
}

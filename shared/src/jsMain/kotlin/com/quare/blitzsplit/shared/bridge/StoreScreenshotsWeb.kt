@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsAssets
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsScreen
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsScreen.Companion.openOverHome
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsSeed
import com.quare.blitzsplit.shared.storescreenshots.storeScreenshotsModuleOf
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import org.w3c.dom.Worker
import kotlin.io.encoding.Base64
import kotlin.js.Promise

private val localHosts = setOf("localhost", "127.0.0.1", "[::1]")

/*
 * Why: the web's screenshots of the sample trip (scripts/readme-hero), as the Apple apps' startForStoreScreenshots
 * takes the App Store's: the trip seeded through the app's own use cases before the first screen reads it, in its
 * language and theme, then the screen named by screenCode opened over Início ("items" stops on the bill, see
 * StoreScreenshotsScreen). Only the dev server calls it (webApp/src/bridge/storeScreenshots.ts), in a browser profile
 * of its own, so the Room it seeds is never anyone's. The dev server and the production build share this library, so
 * it can't be left out of the bundle the way a release framework leaves out the Apple one; anywhere but this machine
 * it starts the app as usual instead, so a call that reached production could never write the sample trip into a
 * person's Room. The photos come from the page as base64 by their path under scripts/store-screenshots/.
 */
fun startBlitzSplitForStoreScreenshots(
    sqliteWorker: Worker,
    languageTag: String,
    isDark: Boolean,
    screenCode: String?,
    photos: dynamic,
): Promise<Unit> {
    if (window.location.hostname !in localHosts) {
        startBlitzSplit(sqliteWorker, languageTag)
        return Promise.resolve(Unit)
    }
    // Why: the trip goes by the app language's code, as the Apple captures' ("en", "pt", "pt-PT", "es-ES").
    val trip = SampleTrips.tripOf(languageTag)
    startBlitzSplitKoin(
        extraModules = listOf(webModuleOf(sqliteWorker, languageTag), storeScreenshotsModuleOf(trip)),
    )
    // Why: a plain JS object, read by key; a dynamic's get() would call a method it hasn't.
    val assets = StoreScreenshotsAssets { path ->
        (if (photos == null) null else photos[path] as? String)?.let { Base64.decode(it) }
    }
    return MainScope().promise {
        val seeded = StoreScreenshotsSeed(trip, assets).createTrip(
            language = AppLanguage.fromTag(languageTag),
            theme = if (isDark) ThemePreference.DARK else ThemePreference.LIGHT,
        )
        val routes = StoreScreenshotsScreen.fromCodeOrNull(screenCode)?.routesOf(seeded).orEmpty()
        val navigator = KoinPlatform.getKoin().get<Navigator>()
        MainScope().launch { navigator.openOverHome(routes) }
    }
}

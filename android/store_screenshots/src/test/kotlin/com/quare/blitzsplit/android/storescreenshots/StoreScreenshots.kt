package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import dev.lucianosantos.storescreenshots.FormFactor
import dev.lucianosantos.storescreenshots.ScreenshotCanvas
import dev.lucianosantos.storescreenshots.ScreenshotStyle
import dev.lucianosantos.storescreenshots.StoreScreenshotsTest
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Every listing screenshot of one device size, one test per screen. The Play classes draw the Compose screen here,
 * under Robolectric, from the sample trip; the Apple ones pass [appleSlot] and frame the PNG the iOS UI tests
 * captured, so the App Store shows the screen SwiftUI really draws. An Apple capture is a whole screen with the
 * simulator's own status bar, so the frame draws none over it.
 */
internal abstract class StoreScreenshots(
    private val appleSlot: AppleScreenshotSlot? = null,
    private val outputSubdir: String? = null,
    formFactor: FormFactor,
    canvas: ScreenshotCanvas? = null,
    mockupFrame: (@Composable (content: @Composable () -> Unit) -> Unit)? = null,
) : StoreScreenshotsTest(
        formFactor = formFactor,
        canvas = canvas,
        style = ScreenshotStyle(
            edgeToEdge = appleSlot != null,
            showStatusBar = appleSlot == null,
            statusBarClock = "9:41",
            mockupFrame = mockupFrame,
        ),
    ) {
    @Test
    fun group() = shot(StoreScreen.GROUP) { GroupScreenshotContent(it) }

    @Test
    fun splitByItems() = shot(StoreScreen.ITEMS) { ItemsScreenshotContent(it) }

    @Test
    fun receipt() = shot(StoreScreen.RECEIPT) { ReceiptScreenshotContent(it) }

    @Test
    fun balances() = shot(StoreScreen.BALANCES) { BalancesScreenshotContent(it) }

    @Test
    fun invite() = shot(StoreScreen.INVITE) { InviteScreenshotContent(it) }

    @Test
    fun statistics() = shot(StoreScreen.STATISTICS) { StatisticsScreenshotContent(it) }

    @Test
    fun home() = shot(StoreScreen.HOME) { HomeScreenshotContent(it) }

    @Test
    fun payersAndCurrencies() = shot(StoreScreen.PAYERS_AND_CURRENCIES) { PayersScreenshotContent(it) }

    @Test
    fun activity() = shot(StoreScreen.ACTIVITY) {
        error("Activity only fills an App Store slot, which frames the iOS capture")
    }

    @Test
    fun lightTheme() = shot(StoreScreen.LIGHT_THEME) { GroupScreenshotContent(it) }

    private companion object {
        // The logo's blue dimmed far down, so the app's own accents stay the brightest thing in each image.
        const val BANNER_BACKGROUND = 0xFF0E2433
    }

    private fun shot(
        screen: StoreScreen,
        content: @Composable (SampleTrip) -> Unit,
    ) {
        assumeFalse("${screen.name} only fills an App Store slot", appleSlot == null && screen.isAppStoreOnly)
        // The iPhone Duo and the Mac are captured only where their simulator or window could be: a run without them
        // skips their slots instead of failing the others.
        appleSlot?.let { assumeTrue("No ${it.directoryName} captures", hasAppleCaptures(it)) }
        // The fixtures' days and times are in UTC, so "Today" and "21:00" read the same on every machine.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        storeLocales.forEach { locale ->
            val copy = bannerCopyOf(screen, locale.language)
            screenshot(
                locales = listOf(locale.tag),
                title = copy.title,
                description = copy.description,
                backgroundColor = Color(BANNER_BACKGROUND),
                subdir = outputSubdir,
                fileName = screen.fileName,
            ) {
                val slot = appleSlot
                if (slot == null) {
                    BlitzSplitTheme(isDark = screen.isDark) {
                        SampleFacesComponent { content(SampleTrips.tripOf(locale.language)) }
                    }
                } else {
                    AppleScreenshotImage(
                        slot = slot,
                        locale = locale.tag,
                        fileName = screen.fileName,
                    )
                }
            }
        }
    }
}

internal class PhoneStoreScreenshots : StoreScreenshots(formFactor = FormFactor.Phone)

internal class Tablet7StoreScreenshots : StoreScreenshots(formFactor = FormFactor.Tablet7)

internal class Tablet10StoreScreenshots : StoreScreenshots(formFactor = FormFactor.Tablet10)

// Dynamic Island (medium display), the iPhone size App Store Connect requires: the 6.7" bezel on a 1206x2622 canvas,
// around an iPhone 17 Pro's 402x874 capture, whose proportions are the 6.7" screen's to within 0.3%.
internal class IPhone63StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPHONE_6_3,
        formFactor = FormFactor.AppleIPhone67,
        outputSubdir = "iphone63",
        canvas = ScreenshotCanvas.px(1206, 2622),
    )

internal class IPhone67StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPHONE_6_7,
        formFactor = FormFactor.AppleIPhone67,
    )

internal class IPad13StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPAD_13,
        formFactor = FormFactor.AppleIPad13,
    )

// The 11" slot: the same bezel on a taller canvas, so Apple doesn't letterbox the 13" one. The screen inside is the
// same size, so it frames the 13" capture.
internal class IPad11StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPAD_13,
        formFactor = FormFactor.AppleIPad13,
        outputSubdir = "ipad11",
        canvas = ScreenshotCanvas.px(1668, 2388),
    )

// The iPhone Duo: the simulator boots it closed, so this is its outer display, in a plain rounded bezel of its own.
internal class IPhoneDuoStoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPHONE_DUO,
        formFactor = FormFactor.AppleIPad13,
        outputSubdir = "iphoneduo",
        canvas = ScreenshotCanvas.px(1398, 2034),
        mockupFrame = { content -> DuoBezelSurface(content) },
    )

// The Mac app's window, on a 2880x1800 canvas (16:10, the largest size the Mac slot takes).
internal class MacStoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.MAC,
        formFactor = FormFactor.AppleIPad13,
        outputSubdir = "mac",
        canvas = ScreenshotCanvas.px(2880, 1800),
        mockupFrame = { content -> MacWindowSurface(content) },
    )

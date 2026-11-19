package com.quare.blitzsplit.android.storescreenshots

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.format.withLatinDigits
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
import java.io.File
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
    bannerFontSizes: BannerFontSizes? = null,
) : StoreScreenshotsTest(
        formFactor = formFactor,
        canvas = canvas,
        style = ScreenshotStyle(
            edgeToEdge = appleSlot != null,
            showStatusBar = appleSlot == null,
            statusBarClock = STATUS_BAR_CLOCK,
            mockupFrame = mockupFrame,
            title = bannerSlotOf(
                fontSize = bannerFontSizes?.title,
                isTitle = true,
            ),
            description = bannerSlotOf(
                fontSize = bannerFontSizes?.description,
                isTitle = false,
            ),
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

        const val OUTPUT_ROOT_PROPERTY = "storeScreenshots.outputRoot"
    }

    private fun shot(
        screen: StoreScreen,
        content: @Composable (SampleTrip) -> Unit,
    ) {
        assumeFalse("${screen.name} only fills an App Store slot", appleSlot == null && screen.isAppStoreOnly)
        // The iPhone Duo, closed and open, and the Mac are captured only where their simulator or window could be: a
        // run without them skips their slots instead of failing the others.
        appleSlot?.let { assumeTrue("No ${it.directoryName} captures of every language", hasAppleCaptures(it)) }
        // The fixtures' days and times are in UTC, so "Today" and "21:00" read the same on every machine.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        storeLocales.forEach { locale ->
            val copy = bannerCopyOf(screen, locale.language)
            // An Apple slot only frames a capture, which got its language and region on the simulator, but Robolectric
            // still draws the banner over it, in a locale it has to take as a resource qualifier: the listing's tag
            // isn't always one (zh-Hant becomes zh-rHant, which it refuses, failing every frame of the device).
            screenshot(
                locales = listOf(locale.drawnLocale),
                title = copy.title,
                description = copy.description,
                backgroundColor = Color(BANNER_BACKGROUND),
                subdir = outputSubdir,
                fileName = screen.fileName,
            ) {
                val slot = appleSlot
                if (slot == null) {
                    // Robolectric lays every locale out left to right, Arabic and Hebrew too. The screen alone is
                    // turned: the device around it, its buttons and its status bar stay as the library draws them,
                    // and an Apple capture was already taken in its language's direction.
                    CompositionLocalProvider(LocalLayoutDirection provides locale.layoutDirection) {
                        LatinDigitsComponent {
                            BlitzSplitTheme(isDark = screen.isDark) {
                                SamplePhotosComponent { content(SampleTrips.tripOf(locale.language)) }
                            }
                        }
                    }
                } else {
                    AppleScreenshotImage(
                        slot = slot,
                        locale = locale.tag,
                        fileName = screen.fileName,
                    )
                }
            }
            moveToListingFolder(
                drawnIn = locale.drawnLocale,
                tag = locale.tag,
            )
        }
    }

    // The library names an image's folder after the locale it drew it in; the collect and stage tasks, and the next
    // person looking, go by the listing's tag. The whole folder moves: the generators run in one fork, one class after
    // the other, so nothing else is writing there.
    private fun moveToListingFolder(
        drawnIn: String,
        tag: String,
    ) {
        if (drawnIn == tag) return
        check(storeLocales.none { it.tag == drawnIn }) {
            "$drawnIn is another set's folder: moving it to $tag would mix the two sets"
        }
        // The library's own rule for where it writes: the property build-logic sets, or the module's folder.
        val outputRoot = File(System.getProperty(OUTPUT_ROOT_PROPERTY) ?: System.getProperty("user.dir"))
        val drawn = outputRoot.resolve(drawnIn)
        check(drawn.isDirectory) {
            "No $drawn to move to $tag: has the library changed where it writes or how it names a locale's folder?"
        }
        drawn.copyRecursively(
            target = outputRoot.resolve(tag),
            overwrite = true,
        )
        check(drawn.deleteRecursively()) { "Couldn't delete $drawn after moving it to $tag" }
    }
}

// A line of the banner at a size of the slot's own, or null to leave the library's: the title's and the description's
// slots of ScreenshotStyle differ in nothing else.
private fun bannerSlotOf(
    fontSize: TextUnit?,
    isTitle: Boolean,
): (@Composable (String) -> Unit)? = fontSize?.let { size ->
    { text ->
        BannerText(
            text = text,
            fontSize = size,
            isTitle = isTitle,
        )
    }
}

internal class PhoneStoreScreenshots : StoreScreenshots(formFactor = FormFactor.Phone)

// Play's tablets lie down, so each screen draws the app's own layout for a tablet lying down: two columns, beside the
// navigation rail where the screen has one. The library's tablet frame only stands upright, but its mockup turns
// (PlayTabletLandscapeSurface). The canvas is 2560x1440: the 16:9 Google Play asks of a landscape screenshot, over the
// 1080 px it wants of a large screen's short side. The two shelves differ in the tablet drawn: 960x600 dp on the 7"
// one, 1280x800 dp on the 10" one, which shows more of each screen.
internal abstract class PlayTabletStoreScreenshots(
    formFactor: FormFactor,
) : StoreScreenshots(
        formFactor = formFactor,
        canvas = ScreenshotCanvas.px(2560, 1440),
        mockupFrame = { content -> PlayTabletLandscapeSurface(formFactor, content) },
        bannerFontSizes = BannerFontSizes(
            title = 40.sp,
            description = 20.sp,
        ),
    )

internal class Tablet7StoreScreenshots : PlayTabletStoreScreenshots(FormFactor.Tablet7)

internal class Tablet10StoreScreenshots : PlayTabletStoreScreenshots(FormFactor.Tablet10)

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

// The 13" iPad lying down, the way it is captured: upright, the sidebar the iPad opens with covers the screen. The
// canvas is the capture's own 2732x2048, one of the two sizes the 13" slot takes lying down, and the library's iPad
// frame only stands upright, so the bezel is this module's (IPadLandscapeBezelSurface). The banner stays on top, as on
// the Mac's wide canvas, which leaves the iPad four fifths of the height.
internal class IPad13StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPAD_13,
        formFactor = FormFactor.AppleIPad13,
        canvas = ScreenshotCanvas.px(2732, 2048),
        mockupFrame = { content -> IPadLandscapeBezelSurface(content) },
        bannerFontSizes = BannerFontSizes(
            title = 44.sp,
            description = 22.sp,
        ),
    )

// The 11" slot: the same iPad on the 11" slot's own canvas, 2388x1668 lying down, so Apple doesn't scale the 13" image
// into it. The device drawn is the same, so it frames the 13" capture, with a banner in step with the smaller canvas.
internal class IPad11StoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPAD_13,
        formFactor = FormFactor.AppleIPad13,
        outputSubdir = "ipad11",
        canvas = ScreenshotCanvas.px(2388, 1668),
        mockupFrame = { content -> IPadLandscapeBezelSurface(content) },
        bannerFontSizes = BannerFontSizes(
            title = 36.sp,
            description = 18.sp,
        ),
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

// The iPhone Duo open: its inner display lying down, the way it opens, in the same plain bezel. Its odd pixel size
// only lands on whole dp at the display's own 3x.
internal class IPhoneDuoOpenStoreScreenshots :
    StoreScreenshots(
        appleSlot = AppleScreenshotSlot.IPHONE_DUO_OPEN,
        formFactor = FormFactor.AppleIPad13,
        outputSubdir = "iphoneduoopen",
        canvas = ScreenshotCanvas.px(2853, 2007, density = 3f),
        mockupFrame = { content -> DuoOpenBezelSurface(content) },
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

/*
 * Why: the app writes its counts with 0-9 through LegacyAppLocale.wrap, which Robolectric never runs, so a "%d" of a
 * string resource would be drawn in Bengali's own digits here.
 */
@Composable
private fun LatinDigitsComponent(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val latinContext = remember(context) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(configuration.locales[0].withLatinDigits())
        context.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalContext provides latinContext,
        LocalConfiguration provides latinContext.resources.configuration,
        content = content,
    )
}

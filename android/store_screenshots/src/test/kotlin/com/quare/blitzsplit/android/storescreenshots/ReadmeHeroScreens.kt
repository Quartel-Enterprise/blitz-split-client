package com.quare.blitzsplit.android.storescreenshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import dev.lucianosantos.storescreenshots.StoreScreenshotsStubApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.TimeZone

/**
 * The Android phone of the README's opening image (scripts/readme-hero): the group's Expenses tab of the English
 * sample trip, the same screen the iPhone, the Mac and the web show beside it, as a bare screen with no frame or banner.
 * The image draws the phone around it, and its status bar over the strip left empty on top. Runs only when
 * `-PreadmeHero` asks for it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// About a Pixel 9's screen: 412x924 dp at 420 dpi, which draws 1081x2425 pixels.
@Config(
    sdk = [35],
    application = StoreScreenshotsStubApplication::class,
    qualifiers = "w412dp-h924dp-420dpi",
)
internal class ReadmeHeroScreens {
    @get:Rule
    val compose = createComposeRule()

    private val statusBarHeight = 36.dp

    @Test
    fun groupDark() = capture(
        isDark = true,
        fileName = "android-dark.png",
    )

    @Test
    fun groupLight() = capture(
        isDark = false,
        fileName = "android-light.png",
    )

    private fun capture(
        isDark: Boolean,
        fileName: String,
    ) {
        val directory = File(checkNotNull(System.getProperty(OUTPUT_DIRECTORY_PROPERTY)) { "Run with -PreadmeHero" })
        // The fixtures' days and times are in UTC, so "Today" and "21:00" read the same on every machine.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        compose.setContent {
            BlitzSplitTheme(isDark = isDark) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Spacer(Modifier.height(statusBarHeight))
                    SamplePhotosComponent { GroupScreenshotContent(SampleTrips.tripOf("en")) }
                }
            }
        }
        compose.waitForIdle()
        directory.mkdirs()
        File(directory, fileName).outputStream().use { output ->
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private companion object {
        const val OUTPUT_DIRECTORY_PROPERTY = "readmeHero.outputDir"
    }
}

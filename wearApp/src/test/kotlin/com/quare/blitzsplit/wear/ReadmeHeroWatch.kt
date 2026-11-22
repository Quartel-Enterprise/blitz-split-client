package com.quare.blitzsplit.wear

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import com.quare.blitzsplit.wear.ui.balances.WatchBalancesContent
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import java.util.TimeZone

/**
 * The Wear OS watch of the README's opening image (scripts/readme-hero): Balances, the watch's home, drawn by the app's
 * own screen from the state its ViewModel would hold for the sample trip. Bare, on a round Pixel Watch 3 45mm's screen
 * (456 px at 320 dpi); the image draws the case around it. Runs only when `-PreadmeHero` asks for it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    application = Application::class,
    qualifiers = "w228dp-h228dp-round-320dpi",
)
internal class ReadmeHeroWatch {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `GIVEN the sample trip totals WHEN Balances is drawn THEN it is written as the README watch`() {
        // Given
        val directory = File(checkNotNull(System.getProperty(OUTPUT_DIRECTORY_PROPERTY)) { "Run with -PreadmeHero" })
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.US)

        // When
        compose.setContent {
            WearTheme {
                // Why: a real watch's window is black; Robolectric's is white, which shows through the list's edges.
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    WatchBalancesContent(
                        state = WearSampleStates.createBalances(SampleTrips.tripOf("en")),
                        onEvent = {},
                    )
                }
            }
        }
        compose.waitForIdle()

        // Then
        directory.mkdirs()
        File(directory, "wear-dark.png").outputStream().use { output ->
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

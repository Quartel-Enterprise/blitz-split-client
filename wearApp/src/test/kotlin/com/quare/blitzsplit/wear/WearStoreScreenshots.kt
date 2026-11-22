package com.quare.blitzsplit.wear

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.wear.compose.material3.AlertDialogContent
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmationDialogContent
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import com.quare.blitzsplit.wear.ui.balances.WatchBalancesContent
import com.quare.blitzsplit.wear.ui.balances.WatchGroupContent
import com.quare.blitzsplit.wear.ui.theme.WearTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Base64
import java.util.Locale
import java.util.TimeZone

/**
 * The Wear OS screenshots of the Google Play listing (scripts/store-screenshots/watch, docs/store-listing-screenshots.md):
 * the watch app's own screens, bare, as Google Play asks (no frame, no text, no background of ours), on a round Pixel
 * Watch 3 45mm's screen: 456 px square at 320 dpi, over 384, the least Play takes. Each language is drawn from its
 * sample trip (SampleTrips, which the iPhone's captures seed): the names, the currency, the language's own strings and
 * number format. The states are those the watch's ViewModels would hold, written by hand since the seed lives in
 * :shared. Runs only when `-PstoreWatch` asks for it, for the languages of `storeWatch.locales` or all.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    application = Application::class,
    qualifiers = "w228dp-h228dp-round-320dpi",
)
internal class WearStoreScreenshots {
    @get:Rule
    val compose = createEmptyComposeRule()

    // Why: the content is set once per language, as the rule allows, and each screen replaces this one.
    private val current = mutableStateOf<@Composable () -> Unit>({})

    @Test
    fun `GIVEN the sample trip of each language WHEN each screen is drawn THEN four square images are written each`() {
        // Given
        val directory = File(checkNotNull(System.getProperty(OUTPUT_DIRECTORY_PROPERTY)) { "Run with -PstoreWatch" })
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val wanted = System
            .getProperty(LOCALES_PROPERTY)
            .orEmpty()
            .split(",")
            .filter(String::isNotBlank)
        val listingLanguages = readListingLanguages()
        val unknown = wanted - listingLanguages.map(WearLanguage::tag).toSet()
        check(unknown.isEmpty()) { "locales.tsv has no $unknown" }
        val languages = listingLanguages.filter { wanted.isEmpty() || it.tag in wanted }

        // When
        languages.forEach { language -> drawLanguage(language, File(directory, language.tag).also(File::mkdirs)) }

        // Then
        assertEquals(
            languages.size * SCREENS_PER_LANGUAGE,
            languages.sumOf { File(directory, it.tag).listFiles { file -> file.extension == "png" }.orEmpty().size },
        )
    }

    private fun drawLanguage(
        language: WearLanguage,
        directory: File,
    ) {
        val previousLocale = Locale.getDefault()
        try {
            drawLanguageIn(language, directory)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun drawLanguageIn(
        language: WearLanguage,
        directory: File,
    ) {
        Locale.setDefault(language.javaLocale)
        RuntimeEnvironment.setQualifiers("+${language.qualifiers}")
        val trip = SampleTrips.tripOf(language.sampleLanguage)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    WearTheme {
                        // Why: a real watch's window is black; Robolectric's is white, which shows through the edges.
                        Box(Modifier.fillMaxSize().background(Color.Black)) { current.value() }
                    }
                }
            }
            capture(directory, "01_balances") {
                WatchBalancesContent(
                    state = WearSampleStates.createBalances(trip, ::photoOf),
                    onEvent = {},
                )
            }
            capture(directory, "02_group", scrollsToEnd = true) {
                WatchGroupContent(
                    state = WearSampleStates.createGroup(trip, ::photoOf),
                    onEvent = {},
                )
            }
            capture(directory, "03_confirm") { ConfirmPaymentDialog(trip) }
            capture(directory, "04_recorded") { RecordedDialog() }
        }
    }

    private fun capture(
        directory: File,
        name: String,
        scrollsToEnd: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        current.value = content
        compose.waitForIdle()
        /*
         * Why: a language whose lines are taller (Japanese, Arabic) leaves a list that doesn't fit, and Wear's
         * ScreenScaffold then folds the EdgeButton away until the list is scrolled to its end, as a person would.
         */
        if (scrollsToEnd) {
            compose.onRoot().performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        // Why: the dialogs' content animates in (the check, the curved text, the buttons' shapes): draw it once it ended.
        compose.mainClock.autoAdvance = false
        repeat(ANIMATION_FRAMES) { compose.mainClock.advanceTimeByFrame() }
        compose.mainClock.autoAdvance = true
        File(directory, "$name.png").outputStream().use { output ->
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    // Why: Robolectric can't capture a dialog's own window, so both dialogs are drawn as the content the app's dialogs show.
    @Composable
    private fun RecordedDialog() {
        val recorded = stringResource(R.string.group_payment_recorded)
        val style = ConfirmationDialogDefaults.curvedTextStyle
        SuccessConfirmationDialogContent(curvedText = { confirmationDialogCurvedText(recorded, style) })
    }

    @Composable
    private fun ConfirmPaymentDialog(trip: SampleTrip) {
        val money = rememberMoneyFormatter(trip.currency)
        val debt = WearSampleStates.createSoccerDebt(trip, ::photoOf)
        AlertDialogContent(
            icon = {
                Icon(
                    Icons.Outlined.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            title = {
                Text(stringResource(R.string.group_confirm_title, money.toText(-debt.amountMinor), debt.name.orEmpty()))
            },
            text = { Text(stringResource(R.string.group_confirm_body, trip.sideGroups[SOCCER].name)) },
            confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = {}) },
            dismissButton = { AlertDialogDefaults.DismissButton(onClick = {}) },
        )
    }

    // Why: a photo of the image bank as a data URI, which the avatars load like any photo's URL; none (initials) when the
    // pilot didn't hand the bank over.
    private fun photoOf(file: String): String? {
        val directory = System.getProperty(ASSETS_DIRECTORY_PROPERTY) ?: return null
        val bytes = File(directory, file).takeIf(File::exists)?.readBytes() ?: return null
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * A listing language, one row of scripts/store-screenshots/locales.tsv: its tag (the folder written), the sample
     * trip's language code and the locale its amounts are written in; the Android resource qualifiers of its strings
     * are its language's, unless `androidQualifiers` says another (values-in, values-iw and the BCP 47 ones are
     * Android's own spelling).
     */
    private data class WearLanguage(
        val tag: String,
        val sampleLanguage: String,
        val localeTag: String,
        val qualifiers: String,
    ) {
        val javaLocale: Locale
            get() = Locale.forLanguageTag(localeTag)
    }

    // Why: the languages whose Android resource qualifiers aren't their tag's language code
    private val androidQualifiers = mapOf(
        "pt-BR" to "pt-rBR",
        "pt-PT" to "pt-rPT",
        "es" to "es-rGQ",
        "es-ES" to "es-rES",
        "id" to "in",
        "zh" to "b+zh+Hans",
        "he" to "iw",
        "zh-Hant" to "b+zh+Hant",
        "sr" to "b+sr+Latn",
    )

    private fun readListingLanguages(): List<WearLanguage> {
        val tsv = File(checkNotNull(System.getProperty(LOCALES_TSV_PROPERTY)) { "Run with -PstoreWatch" })
        return tsv
            .readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { row -> row.split('\t') }
            .map { columns ->
                WearLanguage(
                    tag = columns[0],
                    sampleLanguage = columns[1],
                    localeTag = columns[3].replace('_', '-'),
                    qualifiers = androidQualifiers[columns[0]] ?: columns[0].substringBefore('-'),
                )
            }
    }

    private companion object {
        const val OUTPUT_DIRECTORY_PROPERTY = "storeWatch.outputDir"
        const val ASSETS_DIRECTORY_PROPERTY = "storeWatch.assetsDir"
        const val LOCALES_PROPERTY = "storeWatch.locales"
        const val LOCALES_TSV_PROPERTY = "storeWatch.localesTsv"
        const val SCREENS_PER_LANGUAGE = 4
        const val ANIMATION_FRAMES = 300
        const val SOCCER = 1
    }
}

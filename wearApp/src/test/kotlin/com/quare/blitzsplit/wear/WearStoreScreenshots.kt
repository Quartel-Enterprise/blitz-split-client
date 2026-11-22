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
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupRow
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
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
import kotlin.math.roundToLong

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
        val languages = listingLanguages.filter { wanted.isEmpty() || it.tag in wanted }
        check(languages.isNotEmpty()) { "No language of $wanted in $listingLanguages" }
        checkEveryListingLanguageHasARow(wanted)

        // When
        languages.forEach { language -> drawLanguage(language, File(directory, language.tag).also(File::mkdirs)) }

        // Then
        assertEquals(
            languages.size * SCREENS_PER_LANGUAGE,
            languages.sumOf { File(directory, it.tag).listFiles { file -> file.extension == "png" }.orEmpty().size },
        )
    }

    // Why: stage-play.mjs wants the four images of every language of locales.tsv: one this list lacks fails there, late.
    private fun checkEveryListingLanguageHasARow(wanted: List<String>) {
        if (wanted.isNotEmpty()) return
        val tsv = System.getProperty(LOCALES_TSV_PROPERTY)?.let(::File)?.takeIf(File::exists) ?: return
        val tags = tsv.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.substringBefore('\t') }
        val missing = tags - listingLanguages.map(WearLanguage::tag).toSet()
        check(
            missing.isEmpty(),
        ) { "locales.tsv has $missing, which listingLanguages lacks: add its row (add-language skill)" }
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
                    state = createBalances(trip),
                    onEvent = {},
                )
            }
            capture(directory, "02_group", scrollsToEnd = true) {
                WatchGroupContent(
                    state = createGroup(trip),
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
        val debt = createSoccerDebt(trip)
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

    /*
     * Why: the English trip's amounts (+$128.08 in all, the soccer's $20.00, the boat tour's EUR 24) in the language's
     * currency, by the ratio of its soccer court to the English one, since the seed's own conversion lives in :shared
     * and is private. The boat tour is in the other currency, at the same amount in every language, as the trip has it.
     */
    private fun createScaledAmount(
        trip: SampleTrip,
        englishMinor: Long,
    ): Long = (
        englishMinor * trip.sideGroups[SOCCER].billTotalMinor /
            SampleTrips
                .tripOf("en")
                .sideGroups[SOCCER]
                .billTotalMinor
                .toDouble()
    ).roundToLong()

    private fun createTotals(trip: SampleTrip): List<WatchTotal> = listOf(
        WatchTotal(
            trip.currency,
            netMinor = createScaledAmount(trip, 12_808),
            toReceiveMinor = createScaledAmount(trip, 20_581),
            toPayMinor = createScaledAmount(trip, 7_773),
        ),
        WatchTotal(
            trip.foreignCurrency,
            netMinor = -2_400,
            toReceiveMinor = 0,
            toPayMinor = 2_400,
        ),
    )

    private fun createBalances(trip: SampleTrip): WatchBalancesUiState {
        val soccer = trip.sideGroups[SOCCER]
        val roommates = trip.sideGroups[ROOMMATES]
        return WatchBalancesUiState(
            isLoaded = true,
            totals = createTotals(trip),
            groups = listOf(
                createGroupRow(
                    "1",
                    trip.groupName,
                    SampleTrips.TRIP_PHOTO,
                    AvatarColor.PEACH,
                    trip.currency,
                    createScaledAmount(trip, 8_408),
                ),
                createGroupRow(
                    "2",
                    roommates.name,
                    roommates.photo,
                    AvatarColor.BLUE,
                    trip.currency,
                    createScaledAmount(trip, 6_400),
                ),
                createGroupRow(
                    "1",
                    trip.groupName,
                    SampleTrips.TRIP_PHOTO,
                    AvatarColor.PEACH,
                    trip.foreignCurrency,
                    -2_400,
                ),
                createGroupRow(
                    "3",
                    soccer.name,
                    soccer.photo,
                    AvatarColor.GREEN,
                    trip.currency,
                    createSoccerDebt(trip).amountMinor,
                ),
            ),
            isOnline = true,
            queuedChangeCount = 0,
            lastSyncedAt = null,
            isConfirmingSignOut = false,
            isSignOutScheduled = false,
        )
    }

    private fun createGroupRow(
        id: String,
        name: String,
        photo: String,
        color: AvatarColor,
        currency: GroupCurrency,
        netMinor: Long,
    ): WatchGroupRow = WatchGroupRow(
        groupId = id,
        name = name,
        initials = name.take(2).uppercase(),
        color = color,
        photoUrl = photoOf("groups/$photo.jpg"),
        currency = currency,
        netMinor = netMinor,
        isQueued = false,
    )

    // Why: Friday soccer's court is paid by the second person and split by the four of them: your share is a quarter.
    private fun createSoccerDebt(trip: SampleTrip): WatchDebt {
        val payer = trip.sideGroups[SOCCER].billPayer
        return WatchDebt(
            memberId = "person$payer",
            name = trip.people[payer],
            initials = trip.people[payer].take(1).uppercase(),
            color = AvatarColor.BLUE,
            photoUrl = photoOf("faces/person$payer.jpg"),
            amountMinor = -trip.sideGroups[SOCCER].billTotalMinor / trip.sideGroups[SOCCER].people.size,
        )
    }

    private fun createGroup(trip: SampleTrip): WatchGroupUiState {
        val debt = createSoccerDebt(trip)
        return WatchGroupUiState(
            isLoaded = true,
            groupName = trip.sideGroups[SOCCER].name,
            currency = trip.currency,
            netMinor = debt.amountMinor,
            debts = listOf(debt),
            confirming = null,
            isRecording = false,
            isQueued = false,
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
     * A listing language: its tag (the folder written), the sample trip's language code, the Android resource
     * qualifiers of its strings (values-in, values-iw and the BCP 47 ones are Android's own spelling) and the locale
     * its amounts are written in (scripts/store-screenshots/locales.tsv).
     */
    private data class WearLanguage(
        val tag: String,
        val sampleLanguage: String,
        val qualifiers: String,
        val localeTag: String,
    ) {
        val javaLocale: Locale
            get() = Locale.forLanguageTag(localeTag)
    }

    private val listingLanguages = listOf(
        WearLanguage("en-US", "en", "en", "en-US"),
        WearLanguage("pt-BR", "pt", "pt-rBR", "pt-BR"),
        WearLanguage("pt-PT", "pt-PT", "pt-rPT", "pt-PT"),
        WearLanguage("es", "es", "es-rUS", "es-GQ"),
        WearLanguage("es-ES", "es-ES", "es-rES", "es-ES"),
        WearLanguage("fr", "fr", "fr", "fr-FR"),
        WearLanguage("de", "de", "de", "de-DE"),
        WearLanguage("it", "it", "it", "it-IT"),
        WearLanguage("pl", "pl", "pl", "pl-PL"),
        WearLanguage("nl", "nl", "nl", "nl-NL"),
        WearLanguage("tr", "tr", "tr", "tr-TR"),
        WearLanguage("id", "id", "in", "id-ID"),
        WearLanguage("ru", "ru", "ru", "ru-RU"),
        WearLanguage("uk", "uk", "uk", "uk-UA"),
        WearLanguage("ko", "ko", "ko", "ko-KR"),
        WearLanguage("hi", "hi", "hi", "hi-IN"),
        WearLanguage("ar", "ar", "ar", "ar-SA"),
        WearLanguage("zh", "zh", "b+zh+Hans", "zh-CN"),
        WearLanguage("he", "he", "iw", "he-IL"),
        WearLanguage("th", "th", "th", "th-TH"),
        WearLanguage("vi", "vi", "vi", "vi-VN"),
        WearLanguage("zh-Hant", "zh-Hant", "b+zh+Hant", "zh-TW"),
        WearLanguage("sv", "sv", "sv", "sv-SE"),
        WearLanguage("cs", "cs", "cs", "cs-CZ"),
        WearLanguage("ro", "ro", "ro", "ro-RO"),
        WearLanguage("bn", "bn", "bn", "bn-BD"),
        WearLanguage("ms", "ms", "ms", "ms-MY"),
        WearLanguage("el", "el", "el", "el-GR"),
        WearLanguage("hr", "hr", "hr", "hr-HR"),
        WearLanguage("hu", "hu", "hu", "hu-HU"),
        WearLanguage("ur", "ur", "ur", "ur-PK"),
        WearLanguage("fil", "fil", "fil", "fil-PH"),
        WearLanguage("sr", "sr", "b+sr+Latn", "sr-Latn-RS"),
        WearLanguage("ja", "ja", "ja", "ja-JP"),
    )

    private companion object {
        const val OUTPUT_DIRECTORY_PROPERTY = "storeWatch.outputDir"
        const val ASSETS_DIRECTORY_PROPERTY = "storeWatch.assetsDir"
        const val LOCALES_PROPERTY = "storeWatch.locales"
        const val LOCALES_TSV_PROPERTY = "storeWatch.localesTsv"
        const val SCREENS_PER_LANGUAGE = 4
        const val ANIMATION_FRAMES = 300
        const val ROOMMATES = 0
        const val SOCCER = 1
    }
}

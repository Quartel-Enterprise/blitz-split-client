package com.quare.blitzsplit.android.storescreenshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.feature.registerpayment.RegisterPaymentContent
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.groups.domain.usecase.impl.EncodeQrCodeUseCase
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.profile.domain.model.PixBrCode
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import dev.lucianosantos.storescreenshots.StoreScreenshotsStubApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import software.quare.loadable.toLoaded
import java.io.File
import java.util.TimeZone

/**
 * The bare screens of Google Play's art (scripts/store-screenshots/listing-art): the real Compose screens of the sample
 * trip, with no frame or banner, one PNG per device, language and screen, which compose.mjs lays out as it does the App
 * Store's captures. Runs only when `-PplayArt` asks for it.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    application = StoreScreenshotsStubApplication::class,
)
internal class PlayArtCaptures(
    private val device: PlayArtDevice,
    private val locale: StoreLocale,
    private val screen: PlayArtScreen,
) {
    private val compose = createComposeRule()

    // Why: first, since the compose rule starts its activity with the qualifiers it finds.
    private val qualifiers = object : TestWatcher() {
        override fun starting(description: Description) {
            RuntimeEnvironment.setQualifiers("${locale.qualifier}-${device.qualifiers}")
        }
    }

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(qualifiers).around(compose)

    @Test
    fun capture() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val trip = SampleTrips.tripOf(locale.language)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides locale.layoutDirection) {
                BlitzSplitTheme(isDark = true) {
                    LatinDigitsComponent {
                        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            Spacer(Modifier.height(STATUS_BAR_HEIGHT.dp))
                            SamplePhotosComponent { screen.content(trip) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val output = File(checkNotNull(System.getProperty(OUTPUT_PROPERTY)) { "Run with -PplayArt" })
        val file = File(output, "${device.directory}/${locale.tag}/${screen.fileName}.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { stream ->
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, stream)
        }
    }

    companion object {
        private const val OUTPUT_PROPERTY = "playArt.outputDir"
        private const val STATUS_BAR_HEIGHT = 36
        private const val FULL_QUALITY = 100
        private const val BRAZIL = "pt-BR"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} {2}")
        fun cases(): List<Array<Any>> = artDevicesOf(System.getenv("ART_DEVICES")).flatMap { device ->
            artLocalesOf(System.getenv("ART_LOCALES")).flatMap { locale ->
                PlayArtScreen.entries
                    .filter { it != PlayArtScreen.PAY || locale.tag == BRAZIL }
                    .map { screen -> arrayOf(device, locale, screen) }
            }
        }
    }
}

// ART_LOCALES=en-US,ja draws those listing tags alone, as compose.mjs reads it; unset or empty, every language.
private fun artLocalesOf(tags: String?): List<StoreLocale> = tags
    ?.takeIf { it.isNotBlank() }
    ?.split(',')
    ?.map { it.trim() }
    ?.let { wanted -> storeLocales.filter { it.tag in wanted } }
    ?: storeLocales

// ART_DEVICES=play_phone,play_tablet_10 captures the devices those compose.mjs names are drawn from; unset or empty, all.
private fun artDevicesOf(names: String?): List<PlayArtDevice> = names
    ?.takeIf { it.isNotBlank() }
    ?.split(',')
    ?.map { it.trim() }
    ?.let { wanted -> PlayArtDevice.entries.filter { device -> device.artDevices.any { it in wanted } } }
    ?: PlayArtDevice.entries

/** What Robolectric's qualifiers call a locale: "pt-BR" is "pt-rBR". */
private val StoreLocale.qualifier: String
    get() = drawnLocale.replace("-", "-r")

// Why: "Pagar Carla", the person this device's user owes, with the made-up key the App Store's capture shows.
@Composable
internal fun PayScreenshotContent(trip: SampleTrip) {
    val snackbar = remember { SnackbarHostState() }
    RegisterPaymentContent(
        state = payStateOf(trip),
        presentation = FormPresentation.BottomSheet,
        snackbarHostState = snackbar,
        onWhenClick = {},
        onEvent = {},
    )
}

private fun SampleTrip.personOf(index: Int): PaymentPerson = PaymentPerson(
    id = memberIdOf(index),
    name = people[index],
    initials = initialsOf(index),
    avatarColor = avatarColorOf(index),
    photoUrl = photoUrlOf(index),
    isMe = index == me,
    personKey = "name:$index",
)

private fun payStateOf(trip: SampleTrip): RegisterPaymentUiState {
    val creditor = SampleTrips.CREDITOR
    val debt = trip.debts
        .first {
            it.fromMemberId == memberIdOf(trip.me) && it.toMemberId == memberIdOf(creditor)
        }.amountMinor
    val code = PixBrCode.of(
        key = SampleTrips.PIX_KEY,
        amountMinor = debt,
        receiverName = trip.people[creditor],
    )
    return RegisterPaymentUiState(
        currency = trip.currency,
        showsSats = false,
        from = trip.personOf(trip.me),
        to = trip.personOf(creditor),
        debtMinor = debt,
        groupParts = emptyList(),
        amountText = "",
        hasEditedAmount = false,
        method = SettlementMethod.PIX,
        paidAt = TODAY_MILLIS,
        openedPaidAt = TODAY_MILLIS,
        openedMethod = SettlementMethod.PIX,
        paidDay = RelativeDay.TODAY,
        latestPaidAt = TODAY_MILLIS,
        isOnline = true,
        isSaving = false,
        isLoaded = true,
        receiverKeys = listOf(
            ReceiverKey(
                id = "sample-pix-key",
                type = PaymentKeyType.RANDOM,
                value = SampleTrips.PIX_KEY,
                text = SampleTrips.PIX_KEY,
                isDefault = true,
            ),
        ).toLoaded(),
        areReceiverKeysUnknown = false,
        groupName = trip.groupName,
        keyMethod = PaymentMethod.PIX,
        showsPix = true,
        offersReceiverKeys = true,
        prefersForm = false,
        pixCode = code,
        pixQrCode = EncodeQrCodeUseCase().invoke(code),
        simplifiedNote = null,
        simplifyChange = null,
    )
}

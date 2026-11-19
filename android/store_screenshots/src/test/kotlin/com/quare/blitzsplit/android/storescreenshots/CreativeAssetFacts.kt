package com.quare.blitzsplit.android.storescreenshots

import android.content.res.Configuration
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.SatsText
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import dev.lucianosantos.storescreenshots.StoreScreenshotsStubApplication
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

/**
 * What the App Store's creative assets (scripts/store-screenshots/creative-assets) write on their tickets, in every
 * language of the listings: the sample trip's five people, "You" in the app's own word, and each one's equal part of
 * the dinner in the app's own format. Written as JSON, so the images follow the sample trip. Runs only when
 * `-PcreativeAssets` asks for it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    application = StoreScreenshotsStubApplication::class,
)
internal class CreativeAssetFacts {
    @Test
    fun write() {
        val directory =
            File(checkNotNull(System.getProperty(OUTPUT_DIRECTORY_PROPERTY)) { "Run with -PcreativeAssets" })
        val facts = JSONObject()
        storeLocales.forEach { locale -> facts.put(locale.tag, factsOf(locale)) }
        directory.mkdirs()
        File(directory, FILE_NAME).writeText(facts.toString(JSON_INDENT))
    }

    private fun factsOf(locale: StoreLocale): JSONObject {
        val trip = SampleTrips.tripOf(locale.language)
        val platformLocale = Locale.forLanguageTag(locale.drawnLocale)
        val you = youIn(platformLocale)
        val money = MoneyFormatter(
            currency = trip.currency,
            showsSats = false,
            satsText = SatsText(
                amount = "%1\$s sats",
                symbol = "sats",
            ),
            locale = platformLocale,
        )
        val share = money.toText(trip.equalShareOfDinner)
        val people = JSONArray()
        trip.people.indices.forEach { person ->
            people.put(
                JSONObject()
                    .put("name", if (person == trip.me) you else trip.people[person])
                    .put("share", share),
            )
        }
        return JSONObject().put("people", people)
    }

    private fun youIn(locale: Locale): String {
        val configuration = Configuration(RuntimeEnvironment.getApplication().resources.configuration)
        configuration.setLocale(locale)
        return RuntimeEnvironment
            .getApplication()
            .createConfigurationContext(configuration)
            .getString(R.string.designsystem_you)
    }

    // Why: the part most people pay. A total that doesn't divide evenly leaves one person a little more ("¥5,220" beside
    // four "¥5,218"), which on tickets meant to look equal reads as a mistake.
    private val SampleTrip.equalShareOfDinner: Long
        get() = sharesOf(bills.first { it.items != null }.copy(items = null))
            .values
            .groupingBy { it }
            .eachCount()
            .maxBy { it.value }
            .key

    private companion object {
        const val OUTPUT_DIRECTORY_PROPERTY = "creativeAssets.outputDir"
        const val FILE_NAME = "facts.json"
        const val JSON_INDENT = 2
    }
}

package com.quare.blitzsplit.wear.surface

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.feature.watchbalances.presentation.mapper.WatchGlance
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import com.quare.blitzsplit.wear.MainActivity
import com.quare.blitzsplit.wear.R
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

/*
 * Why: the watch face's complications (15.2): short "+186" with the app's icon, the color left to the face; long
 * "You owe R$ 126,00", or what you receive when you owe nothing. Tapping either opens the app.
 */
class BalanceComplicationService : SuspendingComplicationDataSourceService() {
    // Why: Profile › Language's pick before Android 13, as every screen of the app follows it.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    private val observePeopleBalances: ObservePeopleBalances by inject()
    private val observeBitcoinInSats: ObserveBitcoinInSats by inject()
    private val observeProfile: ObserveProfile by inject()

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> shortTextOf(
            text = "+186",
            description = getString(R.string.complication_name),
        )

        ComplicationType.LONG_TEXT -> longTextOf(getString(R.string.complication_you_owe, "R$ 126,00"))

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val glance = WatchGlance.of(
            hasProfile = observeProfile().first() != null,
            balances = observePeopleBalances().first(),
        )
        val balance = (glance as? WatchGlance.Open)?.let {
            GlanceBalance.of(this, it.total, showsSats = observeBitcoinInSats().first())
        }
        // Why: a watch with no account shows the app's icon alone, no number, until it connects.
        val isConnected = glance != WatchGlance.NotConnected
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortTextOf(
                text = if (isConnected) balance?.shortText ?: "0" else "",
                description = when {
                    !isConnected -> getString(R.string.pair_connect)
                    balance != null -> balance.netText
                    else -> getString(R.string.tile_settled_title)
                },
            )

            ComplicationType.LONG_TEXT -> if (!isConnected) {
                longTextOf(
                    text = "",
                    description = getString(R.string.pair_connect),
                )
            } else {
                longTextOf(
                    when {
                        balance == null -> getString(R.string.tile_settled_title)
                        balance.hasToPay -> getString(R.string.complication_you_owe, balance.toPayText)
                        else -> getString(R.string.complication_owed_to_you, balance.toReceiveText)
                    },
                )
            }

            else -> null
        }
    }

    private fun shortTextOf(
        text: String,
        description: String,
    ): ComplicationData = ShortTextComplicationData
        .Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(description).build(),
        ).setMonochromaticImage(createIcon())
        .setTapAction(openApp())
        .build()

    private fun longTextOf(
        text: String,
        description: String = text,
    ): ComplicationData = LongTextComplicationData
        .Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(description).build(),
        ).setMonochromaticImage(createIcon())
        .setTapAction(openApp())
        .build()

    private fun createIcon(): MonochromaticImage =
        MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_notification)).build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}

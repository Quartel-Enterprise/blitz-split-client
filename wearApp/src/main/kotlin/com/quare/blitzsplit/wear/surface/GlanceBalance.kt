package com.quare.blitzsplit.wear.surface

import android.content.Context
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.SatsText
import com.quare.blitzsplit.android.designsystem.format.withLatinDigits
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalanceDirection
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: how the Tile (15.1) and the complications (15.2) write the open total WatchGlance picks, the one Balances shows
 * on top.
 */
class GlanceBalance(
    private val total: WatchTotal,
    private val money: MoneyFormatter,
    private val showsSats: Boolean,
) {
    // Why: the word and the color the watch's Balances gives the same total, "Even overall" when open pairs offset.
    val direction: WatchBalanceDirection
        get() = total.direction

    val netText: String
        get() = money.toSignedText(total.netMinor)

    val toReceiveText: String
        get() = money.toText(total.toReceiveMinor)

    val toPayText: String
        get() = money.toText(total.toPayMinor)

    val hasToPay: Boolean
        get() = total.toPayMinor > 0

    /*
     * Why: "+186", the short complication's whole units with the sign, since the word and the cents don't fit. Under
     * one unit it keeps the cents ("−0,50"), never a "−0" that hides a debt; bitcoin in sats counts whole sats.
     */
    val shortText: String
        get() {
            val net = abs(total.netMinor)
            val fractionDigits = if (total.currency == GroupCurrency.BTC &&
                showsSats
            ) {
                0
            } else {
                total.currency.fractionDigits
            }
            var minorPerUnit = 1L
            repeat(fractionDigits) { minorPerUnit *= DECIMAL_BASE }
            val format = NumberFormat.getNumberInstance(Locale.getDefault().withLatinDigits())
            val digits = if (net >= minorPerUnit) {
                format.maximumFractionDigits = 0
                format.format(net / minorPerUnit)
            } else {
                format.minimumFractionDigits = fractionDigits
                format.maximumFractionDigits = fractionDigits
                format.format(net.toDouble() / minorPerUnit)
            }
            return when (direction) {
                WatchBalanceDirection.TO_RECEIVE -> "+$digits"
                WatchBalanceDirection.TO_PAY -> "−$digits"
                WatchBalanceDirection.EVEN -> digits
            }
        }

    companion object {
        private const val DECIMAL_BASE = 10

        fun of(
            context: Context,
            total: WatchTotal,
            showsSats: Boolean,
        ): GlanceBalance {
            val money = MoneyFormatter(
                currency = total.currency,
                showsSats = showsSats,
                satsText = SatsText(
                    amount = context.getString(DesignR.string.designsystem_money_sats),
                    symbol = context.getString(DesignR.string.designsystem_money_sats_symbol),
                ),
                locale = Locale.getDefault(),
            )
            return GlanceBalance(total, money, showsSats)
        }
    }
}

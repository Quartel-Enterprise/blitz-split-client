package com.quare.blitzsplit.wear.surface

import android.content.Context
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.SatsText
import com.quare.blitzsplit.android.designsystem.format.withLatinDigits
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalanceDirection
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: what the Tile (15.1) and the complications (15.2) show: the overall balance in the currency with the most
 * groups, the first of PeopleBalances' totals, as the watch's Balances shows it on top; null when nothing is open.
 */
class GlanceBalance(
    private val total: PeopleTotal,
    private val money: MoneyFormatter,
) {
    // Why: the word and the color the watch's Balances gives the same total, "Even overall" when open pairs offset.
    val direction: WatchBalanceDirection
        get() = WatchBalanceDirection.of(total.netMinor)

    val netText: String
        get() = money.toSignedText(total.netMinor)

    val toReceiveText: String
        get() = money.toText(total.toReceiveMinor)

    val toPayText: String
        get() = money.toText(total.toPayMinor)

    val hasToPay: Boolean
        get() = total.toPayMinor > 0

    // Why: "+186", the short complication's whole units with the sign, since the word and the cents don't fit.
    val shortText: String
        get() {
            var minorPerUnit = 1L
            repeat(total.currency.fractionDigits) { minorPerUnit *= DECIMAL_BASE }
            val whole = abs(total.netMinor) / minorPerUnit
            val digits = NumberFormat.getIntegerInstance(Locale.getDefault().withLatinDigits()).format(whole)
            return when {
                total.netMinor > 0 -> "+$digits"
                total.netMinor < 0 -> "−$digits"
                else -> digits
            }
        }

    companion object {
        private const val DECIMAL_BASE = 10

        fun of(
            context: Context,
            balances: PeopleBalances,
        ): GlanceBalance? {
            val total = balances.totals.firstOrNull { it.toReceiveMinor != 0L || it.toPayMinor != 0L } ?: return null
            val money = MoneyFormatter(
                currency = total.currency,
                showsSats = false,
                satsText = SatsText(
                    amount = context.getString(DesignR.string.designsystem_money_sats),
                    symbol = context.getString(DesignR.string.designsystem_money_sats_symbol),
                ),
                locale = Locale.getDefault(),
            )
            return GlanceBalance(total, money)
        }
    }
}

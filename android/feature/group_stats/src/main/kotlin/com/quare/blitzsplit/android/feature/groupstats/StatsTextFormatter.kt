package com.quare.blitzsplit.android.feature.groupstats

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.android.designsystem.format.withGregorianCalendar
import com.quare.blitzsplit.android.designsystem.format.withLatinDigits
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.hours

/*
 * Why: the shared code hands a period's days and the bars as the first instant of each day; the platform writes them
 * in the UI's language ("24–29 de out.", "setembro de 2026", "jul.", "2025").
 */
@Stable
internal class StatsTextFormatter(
    private val locale: Locale,
    private val zone: ZoneId,
) {
    // Why: an end at midnight reads as the day before, so a range ends at noon of its last day.
    private val endOfDayShift = 12.hours.inWholeMilliseconds
    private val intervalLocale = ULocale.forLocale(locale.withLatinDigits().withGregorianCalendar())
    private val dayOfMonth = formatterOf("d")
    private val shortMonth = formatterOf("LLL")
    private val longMonth = formatterOf("LLLL")
    private val longMonthYear = formatterOf("MMMMyyyy")
    private val year = formatterOf("y")
    private val dayMonth = formatterOf("MMMd")

    fun toRangeText(
        style: StatsRangeStyle,
        startMillis: Long,
        endMillis: Long,
    ): String = when (style) {
        StatsRangeStyle.MONTH -> longMonthYear.format(zonedOf(startMillis))
        StatsRangeStyle.DAYS -> toDaysText(startMillis, endMillis)
        StatsRangeStyle.MONTHS -> rangeOf(startMillis, endMillis, MONTHS_SKELETON)
    }

    // Why: the month a period of one month names in "Nenhuma despesa em setembro".
    fun toMonthText(millis: Long): String = longMonth.format(zonedOf(millis))

    fun toYearText(millis: Long): String = year.format(zonedOf(millis))

    /*
     * Why: under the chart a week is its first day ("19 de out."), short enough for several side by side; its whole
     * range ("19 – 25 de out.") overlapped the next one's (beta feedback of 2026-11-14) and stays in the callout and
     * the most expensive week's sentence.
     */
    fun toBarLabel(
        bar: StatsBar,
        grain: StatsGrain,
        isSparse: Boolean,
        today: String,
    ): String = when (grain) {
        StatsGrain.DAY -> if (bar.isToday) today else dayOfMonth.format(zonedOf(bar.startMillis))
        StatsGrain.WEEK -> (if (isSparse) shortMonth else dayMonth).format(zonedOf(bar.startMillis))
        StatsGrain.MONTH -> shortMonth.format(zonedOf(bar.startMillis))
        StatsGrain.YEAR -> year.format(zonedOf(bar.startMillis))
    }

    // Why: what a bar names in a sentence: "28 de out.", "26 de out. – 1 de nov.", "julho" or "2025".
    fun toBarName(
        bar: StatsBar,
        grain: StatsGrain,
    ): String = when (grain) {
        StatsGrain.DAY -> dayMonth.format(zonedOf(bar.startMillis))
        StatsGrain.WEEK -> toWeekText(bar)
        StatsGrain.MONTH -> longMonth.format(zonedOf(bar.startMillis))
        StatsGrain.YEAR -> year.format(zonedOf(bar.startMillis))
    }

    private fun toWeekText(bar: StatsBar): String = rangeOf(bar.startMillis, bar.endMillis, DAYS_SKELETON)

    private fun toDaysText(
        startMillis: Long,
        endMillis: Long,
    ): String {
        val thisYear = ZonedDateTime.now(zone).year
        val isThisYear = zonedOf(startMillis).year == thisYear && zonedOf(endMillis).year == thisYear
        return rangeOf(startMillis, endMillis, if (isThisYear) DAYS_SKELETON else DAYS_YEAR_SKELETON)
    }

    /*
     * Why: what DateUtils.formatDateRange writes ("24–29 de out."), through ICU's interval format itself, so it is
     * written on the Gregorian calendar and 0-9 as the rest of the screen, Thai's own being the Buddhist era.
     */
    private fun rangeOf(
        startMillis: Long,
        endMillis: Long,
        skeleton: String,
    ): String {
        val format = DateIntervalFormat.getInstance(skeleton, intervalLocale)
        format.timeZone = TimeZone.getTimeZone(zone.id)
        return format.format(DateInterval(startMillis, endMillis + endOfDayShift))
    }

    private fun zonedOf(epochMillis: Long): ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)

    // Why: 0-9 in every language, as the amounts (MoneyFormatter); java.time's own digits are already 0-9.
    private fun formatterOf(skeleton: String): DateTimeFormatter =
        locale.withLatinDigits().withGregorianCalendar().let { latin ->
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(latin, skeleton), latin)
        }

    private companion object {
        const val MONTHS_SKELETON = "yMMM"
        const val DAYS_SKELETON = "MMMd"
        const val DAYS_YEAR_SKELETON = "yMMMd"
    }
}

@Composable
internal fun rememberStatsTextFormatter(): StatsTextFormatter {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) {
        StatsTextFormatter(
            locale = locale,
            zone = ZoneId.systemDefault(),
        )
    }
}

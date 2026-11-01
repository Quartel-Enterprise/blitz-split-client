package com.quare.blitzsplit.android.feature.groupstats

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
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
    private val context: Context,
    private val locale: Locale,
    private val zone: ZoneId,
) {
    // Why: DateUtils reads an end at midnight as the day before, so a range ends at noon of its last day.
    private val endOfDayShift = 12.hours.inWholeMilliseconds
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

        StatsRangeStyle.MONTHS -> DateUtils.formatDateRange(
            context,
            startMillis,
            endMillis + endOfDayShift,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_MONTH_DAY or DateUtils.FORMAT_ABBREV_MONTH or
                DateUtils.FORMAT_SHOW_YEAR,
        )
    }

    // Why: the month a period of one month names in "Nenhuma despesa em setembro".
    fun toMonthText(millis: Long): String = longMonth.format(zonedOf(millis))

    fun toYearText(millis: Long): String = year.format(zonedOf(millis))

    fun toBarLabel(
        bar: StatsBar,
        grain: StatsGrain,
        isSparse: Boolean,
        today: String,
    ): String = when (grain) {
        StatsGrain.DAY -> if (bar.isToday) today else dayOfMonth.format(zonedOf(bar.startMillis))
        StatsGrain.WEEK -> if (isSparse) shortMonth.format(zonedOf(bar.startMillis)) else toWeekText(bar)
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

    private fun toWeekText(bar: StatsBar): String = DateUtils.formatDateRange(
        context,
        bar.startMillis,
        bar.endMillis + endOfDayShift,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
    )

    private fun toDaysText(
        startMillis: Long,
        endMillis: Long,
    ): String {
        val thisYear = ZonedDateTime.now(zone).year
        val isThisYear = zonedOf(startMillis).year == thisYear && zonedOf(endMillis).year == thisYear
        val yearFlag = if (isThisYear) DateUtils.FORMAT_NO_YEAR else DateUtils.FORMAT_SHOW_YEAR
        return DateUtils.formatDateRange(
            context,
            startMillis,
            endMillis + endOfDayShift,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or yearFlag,
        )
    }

    private fun zonedOf(epochMillis: Long): ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)

    private fun formatterOf(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
internal fun rememberStatsTextFormatter(): StatsTextFormatter {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    return remember(context, locale) {
        StatsTextFormatter(
            context = context,
            locale = locale,
            zone = ZoneId.systemDefault(),
        )
    }
}

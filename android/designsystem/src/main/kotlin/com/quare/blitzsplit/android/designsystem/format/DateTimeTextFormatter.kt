package com.quare.blitzsplit.android.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Why: the shared code hands days and times as epoch millis; the platform writes them in the UI's language, in the
 * device's time zone and with the 12 or 24 hour clock of Profile's "Time format", the system's by default ("Sex., 24
 * de out.", "28 de out.", "21:30" or "9:30 PM").
 */
@Stable
class DateTimeTextFormatter(
    private val locale: Locale,
    private val zone: ZoneId,
    is24Hour: Boolean,
) {
    // Why: each pattern is resolved on first use, so a caller that only writes times (Time format's examples) pays for one.
    private val weekdayDate by lazy { formatterOf(WEEKDAY_DATE_SKELETON) }
    private val date by lazy { formatterOf(DATE_SKELETON) }
    private val timeSkeleton = if (is24Hour) TIME_24_HOUR_SKELETON else TIME_12_HOUR_SKELETON
    private val time by lazy { formatterOf(timeSkeleton) }

    fun toWeekdayDateText(epochMillis: Long): String = weekdayDate
        .format(zonedOf(epochMillis))
        .replaceFirstChar { it.titlecase(locale) }

    fun toDateText(epochMillis: Long): String = date.format(zonedOf(epochMillis))

    fun toTimeText(epochMillis: Long): String = time.format(zonedOf(epochMillis))

    // Why: a time of day with no date, as a group's reminder keeps it, on the same clock and pattern as the rest.
    fun toTimeText(timeOfDay: LocalTime): String = time.format(timeOfDay)

    private fun zonedOf(epochMillis: Long): ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)

    // Why: 0-9 in every language, as the amounts (MoneyFormatter); java.time's own digits are already 0-9.
    private fun formatterOf(skeleton: String): DateTimeFormatter = locale.withLatinDigits().let { latin ->
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(latin, skeleton), latin)
    }

    private companion object {
        const val WEEKDAY_DATE_SKELETON = "EEEMMMd"
        const val DATE_SKELETON = "MMMd"
        const val TIME_24_HOUR_SKELETON = "Hm"
        const val TIME_12_HOUR_SKELETON = "hm"
    }
}

@Composable
fun rememberDateTimeTextFormatter(): DateTimeTextFormatter {
    val locale = LocalLocale.current.platformLocale
    val cache = LocalFormatterCache.current
    if (cache != null && cache.isFor(locale)) return cache.dates
    val is24Hour = rememberIs24HourClock()
    return remember(locale, is24Hour) {
        DateTimeTextFormatter(
            locale = locale,
            zone = ZoneId.systemDefault(),
            is24Hour = is24Hour,
        )
    }
}

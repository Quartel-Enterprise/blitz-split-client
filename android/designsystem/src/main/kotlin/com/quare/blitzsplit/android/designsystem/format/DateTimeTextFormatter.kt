package com.quare.blitzsplit.android.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Why: the shared code hands days and times as epoch millis; the platform writes them in the UI's language, in the
 * device's time zone and with its 12 or 24 hour clock ("Sex., 24 de out.", "28 de out.", "21:30" or "9:30 PM").
 */
@Stable
class DateTimeTextFormatter(
    private val locale: Locale,
    private val zone: ZoneId,
    is24Hour: Boolean,
) {
    private val weekdayDate = formatterOf(WEEKDAY_DATE_SKELETON)
    private val date = formatterOf(DATE_SKELETON)
    private val time = formatterOf(if (is24Hour) TIME_24_HOUR_SKELETON else TIME_12_HOUR_SKELETON)

    fun toWeekdayDateText(epochMillis: Long): String = weekdayDate
        .format(zonedOf(epochMillis))
        .replaceFirstChar { it.titlecase(locale) }

    fun toDateText(epochMillis: Long): String = date.format(zonedOf(epochMillis))

    fun toTimeText(epochMillis: Long): String = time.format(zonedOf(epochMillis))

    private fun zonedOf(epochMillis: Long): ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)

    private fun formatterOf(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

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
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) {
        DateTimeTextFormatter(
            locale = locale,
            zone = ZoneId.systemDefault(),
            is24Hour = is24Hour,
        )
    }
}

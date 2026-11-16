package com.quare.blitzsplit.android.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quare.blitzsplit.core.model.TimeFormatPreference

// Why: Profile's "Time format", which the app's root provides from AppViewModel; a preview keeps the system's clock.
val LocalTimeFormat = staticCompositionLocalOf { TimeFormatPreference.SYSTEM }

/*
 * Why: whether every time of day draws on 24 hours ("14:30") or 12 with AM and PM ("2:30 PM"): the pick, or the
 * system's own clock while it follows the system. The texts and the time picker ask the same thing, so they agree.
 */
@Composable
fun rememberIs24HourClock(): Boolean = LocalTimeFormat.current.isTwentyFourHour(rememberIsSystem24HourClock())

/*
 * Why: the system's own 12 or 24 hours, read again each time the app comes back, since it changes in the system's
 * settings while the app is away.
 */
@Composable
fun rememberIsSystem24HourClock(): Boolean {
    val context = LocalContext.current
    var isSystem24Hour by remember(context) { mutableStateOf(DateFormat.is24HourFormat(context)) }
    LifecycleResumeEffect(context) {
        isSystem24Hour = DateFormat.is24HourFormat(context)
        onPauseOrDispose {}
    }
    return isSystem24Hour
}

fun TimeFormatPreference.isTwentyFourHour(isSystem24Hour: Boolean): Boolean = when (this) {
    TimeFormatPreference.SYSTEM -> isSystem24Hour
    TimeFormatPreference.TWENTY_FOUR_HOUR -> true
    TimeFormatPreference.TWELVE_HOUR -> false
}

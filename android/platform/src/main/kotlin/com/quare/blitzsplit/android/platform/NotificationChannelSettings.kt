package com.quare.blitzsplit.android.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

// Why: the ones of these channels the person silenced in the system's settings; one not created yet isn't silenced.
fun Context.findSilencedNotificationChannels(channelIds: Collection<String>): Set<String> =
    // Why: the app's channels in one call to the system, rather than one per channel on each return.
    NotificationManagerCompat
        .from(this)
        .notificationChannelsCompat
        .filter { it.id in channelIds && it.importance == NotificationManagerCompat.IMPORTANCE_NONE }
        .mapTo(mutableSetOf()) { it.id }

// Why: the settings of one channel alone, where the person turns it back on.
fun Context.openNotificationChannelSettings(channelId: String) {
    try {
        startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
        )
    } catch (_: ActivityNotFoundException) {
        // Why: a build with no screen for one channel's settings: the app's own notification settings instead.
        openAppNotificationSettings()
    }
}

// Why: the app's notification settings; a build with none leaves the tap without effect rather than crashing.
fun Context.openAppNotificationSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    } catch (_: ActivityNotFoundException) {
        // Why: nothing left to open on such a build.
    }
}

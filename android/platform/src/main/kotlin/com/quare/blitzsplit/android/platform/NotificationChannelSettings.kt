package com.quare.blitzsplit.android.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

// Why: the ones of these channels the person silenced in the system's settings; one not created yet isn't silenced.
fun Context.findSilencedNotificationChannels(channelIds: Collection<String>): Set<String> {
    val manager = NotificationManagerCompat.from(this)
    return channelIds
        .filter { id ->
            manager.getNotificationChannelCompat(id)?.importance ==
                NotificationManagerCompat.IMPORTANCE_NONE
        }.toSet()
}

// Why: the settings of one channel alone, where the person turns it back on.
fun Context.openNotificationChannelSettings(channelId: String) {
    startActivity(
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
    )
}

package com.quare.blitzsplit.watch

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quare.blitzsplit.MainActivity
import com.quare.blitzsplit.R
import com.quare.blitzsplit.push.canPostNotifications

/*
 * Why: "Pixel Watch connected to your account" (15.15), information only, with no buttons; a tap opens the app. Its own
 * channel, "Watch", since it belongs to none of Notifications' rows and the person may silence it alone.
 */
object WatchConnectedNotice {
    private const val CHANNEL_ID = "watch"
    private const val NOTIFICATION_ID = 15_15

    @SuppressLint("MissingPermission") // Why: canPostNotifications() checks it, on the versions that have it.
    fun show(
        context: Context,
        watchName: String?,
    ) {
        if (!context.canPostNotifications()) return
        createChannel(context)
        val title = watchName
            ?.let { context.getString(R.string.watch_connected_title, it) }
            ?: context.getString(R.string.watch_connected_title_unnamed)
        val text = context.getString(R.string.watch_connected_text)
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.watch_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

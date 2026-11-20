package com.quare.blitzsplit.watch

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quare.blitzsplit.MainActivity
import com.quare.blitzsplit.R

/*
 * Why: "Pixel Watch connected to your account" (15.15), information only, with no buttons; a tap opens the app. Its own
 * channel, "Watch", since it belongs to none of Notifications' rows and the person may silence it alone.
 */
object WatchConnectedNotice {
    private const val CHANNEL_ID = "watch"
    private const val NOTIFICATION_ID = 15_15

    @SuppressLint("MissingPermission") // Why: canNotify() checks it, on the versions that have it.
    fun show(
        context: Context,
        watchName: String?,
    ) {
        if (!canNotify(context)) return
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

    // Why: before Android 13 the permission doesn't exist and notifications are on until the settings turn them off.
    private fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

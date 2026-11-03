package com.quare.blitzsplit.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quare.blitzsplit.MainActivity
import com.quare.blitzsplit.R
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import org.json.JSONArray
import org.koin.android.ext.android.inject

/*
 * Why: FCM sends data messages, which reach the app open or closed, so the app words and shows each notification
 * itself, in the channel of its toggle. A tap opens its link in MainActivity, which takes it like an invite's.
 */
class BlitzSplitMessagingService : FirebaseMessagingService() {
    private val setPushDevice: SetPushDevice by inject()

    override fun onNewToken(token: String) {
        setPushDevice(
            PushDevice(
                token = token,
                platform = PushPlatform.ANDROID,
                apnsEnvironment = null,
                webP256dh = null,
                webAuth = null,
            ),
        )
    }

    @SuppressLint("MissingPermission") // Why: canNotify() checks it, on the versions that have it.
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val kind = PushMessage.ofKind(data[KIND]) ?: return
        // Why: words that don't fit this version's sentence (a newer server's) drop the notification, never the app.
        val text = runCatching { buildText(kind, data[ARGS]) }.getOrNull() ?: return
        val title = data[TITLE].orEmpty().ifEmpty { getString(R.string.push_reminder_title) }
        val tag = data[EVENT_ID] ?: kind.kind
        val link = data[LINK]
        if (!canNotify()) return
        val notification = NotificationCompat
            .Builder(this, kind.channel.id)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply { link?.let { setContentIntent(openIntent(it, tag)) } }
            .build()
        NotificationManagerCompat.from(this).notify(tag, 0, notification)
    }

    // Why: before Android 13 the permission doesn't exist and notifications are on until the settings turn them off.
    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun buildText(
        kind: PushMessage,
        args: String?,
    ): String {
        val words = args?.let(::JSONArray)?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty()
        return getString(kind.textRes, *words.toTypedArray())
    }

    private fun openIntent(
        link: String,
        tag: String,
    ): PendingIntent = PendingIntent.getActivity(
        this,
        tag.hashCode(),
        Intent(Intent.ACTION_VIEW, link.toUri(), this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val KIND = "kind"
        const val TITLE = "title"
        const val ARGS = "args"
        const val LINK = "link"
        const val EVENT_ID = "event_id"
    }
}

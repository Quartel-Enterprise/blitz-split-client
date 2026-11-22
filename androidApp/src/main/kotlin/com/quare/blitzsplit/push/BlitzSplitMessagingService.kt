package com.quare.blitzsplit.push

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quare.blitzsplit.MainActivity
import com.quare.blitzsplit.R
import com.quare.blitzsplit.android.push.PushMessage
import com.quare.blitzsplit.android.push.PushPayload
import com.quare.blitzsplit.android.push.PushTokens
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePushEnabled
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.seconds
import com.quare.blitzsplit.android.push.R as PushR

/*
 * Why: FCM sends data messages, which reach the app open or closed, so the app words and shows each notification
 * itself, in the channel of its toggle. A tap opens its link in MainActivity, which takes it like an invite's.
 */
class BlitzSplitMessagingService : FirebaseMessagingService() {
    private val setPushDevice: SetPushDevice by inject()
    private val observePushEnabled: ObservePushEnabled by inject()
    private val syncNow: SyncNow by inject()

    // Why: before Android 13 the notifications built here are in Profile › Language's pick, as the screens are.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    /*
     * Why: Firebase 26 deprecates the token for register()/onRegistered(), which need the installation id opted in
     * by a manifest flag and hand the server another kind of id; the token is what `push_devices` stores.
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        PushTokens.handOver(setPushDevice, token)
    }

    @SuppressLint("MissingPermission") // Why: canPostNotifications() checks it, on the versions that have it.
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val kind = PushMessage.ofKind(data[PushPayload.KIND]) ?: return
        // Why: words that don't fit this version's sentence (a newer server's) drop the notification, never the app.
        val text = kind.toText(this, data[PushPayload.ARGS]) ?: return
        // Why: only a reminder comes without a group to name; anything else missing one still names the app.
        val fallbackTitle = if (kind.isReminder) {
            PushR.string.push_reminder_title
        } else {
            R.string.app_name
        }
        val title = data[PushPayload.TITLE].orEmpty().ifEmpty { getString(fallbackTitle) }
        val tag = data[PushPayload.EVENT_ID] ?: kind.kind
        val link = data[PushPayload.LINK]
        if (!canPostNotifications() || !isPushEnabled()) return
        val notification = NotificationCompat
            .Builder(this, kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply { link?.let { setContentIntent(openIntent(it, tag)) } }
            .build()
        NotificationManagerCompat.from(this).notify(tag, 0, notification)
        syncForTheTap()
    }

    /*
     * Why: the tap opens the bill only once it is on the device (AppViewModel.openPushLink waits up to 5 seconds for a
     * sync), so the sync starts as the notification shows and the bill is usually there before the tap. After notify,
     * so the notification never waits on it. Blocking keeps the process alive: FCM allows a high-priority message
     * about 10 seconds of work, and what a cut sync already saved stays in Room.
     */
    private fun syncForTheTap() {
        runBlocking { withTimeoutOrNull(SYNC_ON_ARRIVAL_SECONDS.seconds) { syncNow() } }
    }

    /*
     * Why: Notifications' "Push notifications" switch off takes the token off the server only once online and only if
     * that call succeeds, so a push still sent meanwhile is dropped here. onMessageReceived runs off the main thread,
     * and the read is one Room row.
     */
    private fun isPushEnabled(): Boolean = runBlocking { observePushEnabled().first() }

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
        const val SYNC_ON_ARRIVAL_SECONDS = 9
    }
}

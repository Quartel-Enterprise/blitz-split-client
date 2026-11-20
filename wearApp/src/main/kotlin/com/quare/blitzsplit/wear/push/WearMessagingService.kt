package com.quare.blitzsplit.wear.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quare.blitzsplit.android.push.PushMessage
import com.quare.blitzsplit.android.push.PushPayload
import com.quare.blitzsplit.android.push.PushTokens
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePushEnabled
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import com.quare.blitzsplit.wear.MainActivity
import com.quare.blitzsplit.wear.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import com.quare.blitzsplit.android.push.R as PushR

/*
 * Why: the watch's own pushes of Activity (15.7), worded on the watch as the phone words them. "X marked that they
 * paid you" has "Received" and "View group"; every other opens the app.
 */
class WearMessagingService : FirebaseMessagingService() {
    private val setPushDevice: SetPushDevice by inject()
    private val observePushEnabled: ObservePushEnabled by inject()

    // Why: someone recorded that they paid you, which "Received" acknowledges on the watch.
    private val markedPaidToYou = setOf(PushMessage.PAYMENT_PAID_YOU, PushMessage.PAYMENT_TO_YOU)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(newBase))
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        PushTokens.handOver(setPushDevice, token)
    }

    @SuppressLint("MissingPermission") // Why: canNotify() checks it.
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val kind = PushMessage.ofKind(data[PushPayload.KIND]) ?: return
        val text = kind.toText(this, data[PushPayload.ARGS]) ?: return
        val fallbackTitle = if (kind.isReminder) PushR.string.push_reminder_title else R.string.app_name
        val title = data[PushPayload.TITLE].orEmpty().ifEmpty { getString(fallbackTitle) }
        val eventId = data[PushPayload.EVENT_ID]
        val tag = eventId ?: kind.kind
        if (!canNotify() || !isPushEnabled()) return
        val builder = NotificationCompat
            .Builder(this, kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(tag))
        if (kind in markedPaidToYou) {
            builder
                .addAction(0, getString(R.string.push_received), ReceivedReceiver.intentOf(this, tag, eventId))
                .addAction(0, getString(R.string.push_view_group), openAppIntent(tag))
        }
        NotificationManagerCompat.from(this).notify(tag, 0, builder.build())
    }

    private fun isPushEnabled(): Boolean = runBlocking { observePushEnabled().first() }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

    private fun openAppIntent(tag: String): PendingIntent = PendingIntent.getActivity(
        this,
        tag.hashCode(),
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

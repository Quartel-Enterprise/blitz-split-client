package com.quare.blitzsplit.wear.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.quare.blitzsplit.android.push.PushChannelIds
import com.quare.blitzsplit.android.push.PushTokens
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.wear.BuildConfig
import com.quare.blitzsplit.wear.R

/*
 * Why: the watch's own pushes (FCM), not the phone's mirrored notifications: it registers its token for the account
 * signed in on it, as every device does. A build without google-services.json has none.
 */
object WearPushSetup {
    fun start(
        context: Context,
        setPushDevice: SetPushDevice,
    ) {
        if (!BuildConfig.HAS_FIREBASE) return
        createChannels(context)
        // Why: see the phone's BlitzSplitMessagingService.onNewToken; the token is what push_devices stores.
        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            PushTokens.handOver(setPushDevice, token)
        }
    }

    // Why: one channel per row the watch shows, with the phone's ids, so the system's settings silence them alike.
    private fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            PushChannelIds.NEW_EXPENSE to R.string.push_channel_bills,
            PushChannelIds.PAYMENT_MARKED to R.string.push_channel_payments,
            PushChannelIds.COMMENTS to R.string.push_channel_comments,
            PushChannelIds.INVITES to R.string.push_channel_people,
            PushChannelIds.PAYMENT_REQUESTS to R.string.push_channel_requests,
            PushChannelIds.SETTLE_REMINDERS to R.string.push_channel_reminders,
        ).forEach { (id, name) ->
            manager.createNotificationChannel(
                NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}

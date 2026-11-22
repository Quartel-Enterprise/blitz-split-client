package com.quare.blitzsplit.wear.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.quare.blitzsplit.android.push.PushChannel
import com.quare.blitzsplit.android.push.PushTokens
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.wear.BuildConfig

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
        // Why: the phone's channels, names and ids alike, so the system's settings silence them as on the phone.
        PushChannel.createAll(context)
        // Why: see the phone's BlitzSplitMessagingService.onNewToken; the token is what push_devices stores.
        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            PushTokens.handOver(setPushDevice, token)
        }
    }
}

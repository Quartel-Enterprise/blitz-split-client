package com.quare.blitzsplit.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.quare.blitzsplit.BuildConfig
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice

/*
 * Why: Atividade's push notifications go through FCM, which starts with the build's google-services.json, so a build
 * without it runs as before, with no notifications.
 */
object PushSetup {
    val isConfigured: Boolean
        get() = BuildConfig.HAS_FIREBASE

    // Why: the token on every launch, since FCM may have changed it while the app was closed.
    fun start(
        context: Context,
        setPushDevice: SetPushDevice,
    ) {
        if (!isConfigured) return
        PushChannel.createAll(context)
        // Why: see BlitzSplitMessagingService.onNewToken; the token stays until push moves to registration ids.
        @Suppress("DEPRECATION")
        val token = FirebaseMessaging.getInstance().token
        token.addOnSuccessListener { value -> handOver(setPushDevice, value) }
    }

    // Why: the one place an FCM token becomes this device's PushDevice, at launch and when FCM renews it.
    fun handOver(
        setPushDevice: SetPushDevice,
        token: String,
    ) {
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
}

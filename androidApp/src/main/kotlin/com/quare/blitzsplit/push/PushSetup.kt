package com.quare.blitzsplit.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.quare.blitzsplit.BuildConfig
import com.quare.blitzsplit.android.feature.notificationsettings.PushChannel
import com.quare.blitzsplit.android.push.PushTokens
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
        token.addOnSuccessListener { value -> PushTokens.handOver(setPushDevice, value) }
    }
}

/*
 * Why: whether a notification the app posts itself (a push, the watch's notice) shows. Before Android 13 the permission
 * doesn't exist and notifications are on until the settings turn them off.
 */
internal fun Context.canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

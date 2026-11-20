package com.quare.blitzsplit.android.push

import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice

object PushTokens {
    // Why: the one place an FCM token becomes this device's PushDevice, the phone's and the watch's alike.
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

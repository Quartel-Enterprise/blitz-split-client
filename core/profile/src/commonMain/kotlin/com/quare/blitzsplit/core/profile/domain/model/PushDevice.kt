package com.quare.blitzsplit.core.profile.domain.model

/*
 * Why: where the push service reaches this device (FCM, APNs, a browser's Web Push), which the server sends Atividade's
 * notifications to while an account is signed in here. Each platform hands it over as its service gives it.
 */
data class PushDevice(
    val token: String,
    val platform: PushPlatform,
    // Why: APNs only. A debug build's token is the sandbox's, which production refuses.
    val apnsEnvironment: ApnsEnvironment?,
    // Why: Web Push only: the browser's keys each message is encrypted for.
    val webP256dh: String?,
    val webAuth: String?,
)

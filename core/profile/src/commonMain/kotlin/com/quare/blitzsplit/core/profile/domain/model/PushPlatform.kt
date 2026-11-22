package com.quare.blitzsplit.core.profile.domain.model

enum class PushPlatform {
    ANDROID,
    IOS,
    MACOS,

    // Why: the Apple Watch app's own APNs token, which the server sends with the watch app's bundle id as its topic.
    WATCHOS,
    WEB,
}

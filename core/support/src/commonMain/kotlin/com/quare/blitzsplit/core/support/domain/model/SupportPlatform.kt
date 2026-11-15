package com.quare.blitzsplit.core.support.domain.model

// Why: the app the email comes from, in a type of this module's own so the UIs that draw it need no other module.
enum class SupportPlatform {
    ANDROID,
    IOS,
    MACOS,
    WEB,
}

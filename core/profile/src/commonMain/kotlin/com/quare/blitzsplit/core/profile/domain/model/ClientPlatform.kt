package com.quare.blitzsplit.core.profile.domain.model

/*
 * Why: the app this code runs in, recorded on the server on each sync (`record_platform_seen`), so the web and the
 * Mac know the account already uses the phone app and stop inviting to it.
 */
enum class ClientPlatform {
    ANDROID,
    IOS,
    MACOS,
    WEB,
}

// Why: each target is one app, so the platform is known at compile time rather than handed over by the UI.
expect val currentClientPlatform: ClientPlatform

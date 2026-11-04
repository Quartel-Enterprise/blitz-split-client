package com.quare.blitzsplit.core.inappupdate.domain.model

/*
 * Why: how this platform gets a new version. Android downloads it inside the app through Google Play, iOS and macOS
 * send to the App Store from a sheet, and the web always serves the latest, so it never checks.
 */
enum class UpdateMechanism {
    GOOGLE_PLAY,
    APP_STORE,
    NONE,
}

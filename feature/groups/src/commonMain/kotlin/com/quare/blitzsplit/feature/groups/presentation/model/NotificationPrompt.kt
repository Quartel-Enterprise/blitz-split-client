package com.quare.blitzsplit.feature.groups.presentation.model

// Why: Home's notification card, which each platform words its way: what "Turn on" does follows from it.
enum class NotificationPrompt {
    // Why: never asked: "Turn on" brings the system's prompt, so "Not now" never spends its one chance.
    SOFT_ASK,

    // Why: Android, refused once: "Turn on" asks the system again, with why the app needs it.
    ASK_AGAIN,

    // Why: only the system settings can allow them: "Turn on" opens them (the web says where instead).
    BLOCKED,
}

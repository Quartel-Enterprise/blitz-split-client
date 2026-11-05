package com.quare.blitzsplit.feature.profile.presentation.model

/*
 * Why: Profile's "App on your phone" (the Mac's Settings › iPhone, 14.4), on the web and the Mac only. The row always
 * stays, as the way to download later; it reads the same invite as Home's card.
 */
data class ProfileAppInvite(
    val status: ProfileAppInviteStatus,
    // Why: a phone's browser shows no code: "Download the app" opens this link, which picks the phone's store.
    val link: String,
    // Why: the Mac says "iPhone" when that is where the account uses the app, and "phone" otherwise.
    val usesIphone: Boolean,
)

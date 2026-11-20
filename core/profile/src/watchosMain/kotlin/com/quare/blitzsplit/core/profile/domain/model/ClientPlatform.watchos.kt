package com.quare.blitzsplit.core.profile.domain.model

/*
 * Why: the server knows the four platforms only, and an Apple Watch app is installed from the iPhone's App Store app,
 * so the account seen on a watch already has the phone app. A platform of its own waits for the server to accept it.
 */
actual val currentClientPlatform: ClientPlatform = ClientPlatform.IOS

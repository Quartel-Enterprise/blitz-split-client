package com.quare.blitzsplit.core.watch.link

/*
 * Why: what the phone and its paired watch say to each other (Wear OS's Data Layer messages, later watchOS's
 * WatchConnectivity), named once so both apps agree. Every payload is UTF-8 text.
 */
object WatchLink {
    // Why: the watch hands the phone its pairing code, which the phone approves with its own session.
    const val PAIR_PATH = "/blitzsplit/watch/pair"

    // Why: the phone's answer about that code, one of the REPLY_ values.
    const val PAIR_REPLY_PATH = "/blitzsplit/watch/pair-reply"

    // Why: the phone signed out of the user id in the payload, so its watch signs out and erases its data (15.13).
    const val SIGNED_OUT_PATH = "/blitzsplit/watch/signed-out"

    const val REPLY_APPROVED = "approved"
    const val REPLY_LOCAL_ONLY = "local_only"
    const val REPLY_FAILED = "failed"

    // Why: the capabilities each app declares, so the other finds the right node among every paired device.
    const val PHONE_CAPABILITY = "blitzsplit_phone"
    const val WATCH_CAPABILITY = "blitzsplit_watch"
}

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.watch.link.WatchLink

/*
 * Why: WatchLink's paths and replies for Swift, which doesn't see :core:watch: the iPhone and the Apple Watch say them
 * to each other over WatchConnectivity as a dictionary of two strings, "path" and "payload".
 */
object AppleWatchLink {
    const val PATH_KEY = "path"
    const val CODE_KEY = "code"
    const val REPLY_KEY = "reply"
    const val USER_ID_KEY = "userId"
    const val PAIR_PATH = WatchLink.PAIR_PATH
    const val PAIR_REPLY_PATH = WatchLink.PAIR_REPLY_PATH
    const val SIGNED_OUT_PATH = WatchLink.SIGNED_OUT_PATH
    const val REPLY_APPROVED = WatchLink.REPLY_APPROVED
    const val REPLY_LOCAL_ONLY = WatchLink.REPLY_LOCAL_ONLY
    const val REPLY_FAILED = WatchLink.REPLY_FAILED
}

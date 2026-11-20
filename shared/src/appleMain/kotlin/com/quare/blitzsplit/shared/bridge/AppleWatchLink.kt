package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.watch.link.WatchLink

/*
 * Why: WatchLink's paths and replies for Swift, which doesn't see :core:watch. Over WatchConnectivity the watch sends
 * [PATH_KEY: PAIR_PATH, CODE_KEY: code] and the iPhone answers [REPLY_KEY: one of the REPLY_ values]; the iPhone tells
 * its watch of a sign out with transferUserInfo [PATH_KEY: SIGNED_OUT_PATH, USER_ID_KEY: userId].
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

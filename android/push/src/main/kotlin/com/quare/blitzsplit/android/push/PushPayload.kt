package com.quare.blitzsplit.android.push

// Why: the keys of an FCM data message the server's push function sends, read the same by the phone and the watch.
object PushPayload {
    const val KIND = "kind"
    const val TITLE = "title"
    const val ARGS = "args"
    const val LINK = "link"
    const val EVENT_ID = "event_id"
}

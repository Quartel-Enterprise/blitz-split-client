package com.quare.blitzsplit.android.push

/*
 * Why: the ids of the channels, one per row of Notifications (PushChannel of :android:feature:notification_settings
 * names them from these); the watch posts to the same ids, so a kind lands in the same row on both.
 */
object PushChannelIds {
    const val NEW_EXPENSE = "new_expense"
    const val PAYMENT_MARKED = "payment_marked"
    const val COMMENTS = "comments"
    const val INVITES = "invites"
    const val PAYMENT_REQUESTS = "payment_requests"
    const val SETTLE_REMINDERS = "settle_reminders"
}

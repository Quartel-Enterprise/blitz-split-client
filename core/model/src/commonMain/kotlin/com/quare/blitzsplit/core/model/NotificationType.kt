package com.quare.blitzsplit.core.model

/*
 * Why: the rows of Notifications, in Activity's filter order and then the weekly reminder, so each kind of Activity
 * sits under exactly one of them.
 */
enum class NotificationType(
    // Why: the weekly reminder is a push only, never an Activity event, so it has no in-app channel.
    val hasInApp: Boolean = true,
) {
    // Why: a bill created, edited or deleted.
    BILLS,

    // Why: a payment recorded or undone, and "Simplify debts" turned on or off, which changes who pays whom.
    PAYMENTS,
    COMMENTS,

    // Why: someone added, joining, leaving, removed, released or renamed, a group created, edited or deleted, its invite link.
    PEOPLE_AND_GROUPS,
    PAYMENT_REQUESTS,

    // Why: "You have an open balance in Apt 302", every Monday.
    BALANCE_REMINDER(hasInApp = false),
}

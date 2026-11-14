package com.quare.blitzsplit.core.account.domain.usecase

/*
 * Why: Analytics' user id is the account's (anonymous, Google or Apple), so one person's events join across their
 * phone, Mac and web and survive a reinstall; none without an account, and cleared on sign out or deletion. Suspends
 * until its scope ends.
 */
fun interface FollowAnalyticsUserId {
    suspend operator fun invoke()
}

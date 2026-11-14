package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.account.domain.usecase.FollowAnalyticsUserId
import com.quare.blitzsplit.core.settings.domain.usecase.FollowUsageDataSharing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * Why: Profile's "Share usage data" and the account's id reach Analytics for the app's whole life, from Koin's start,
 * so a process started without a screen (a push, a background start) follows them too, and closing the Activity
 * doesn't stop them.
 */
internal class AnalyticsChoices(
    private val followUsageDataSharing: FollowUsageDataSharing,
    private val followAnalyticsUserId: FollowAnalyticsUserId,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { followUsageDataSharing() }
        scope.launch { followAnalyticsUserId() }
    }
}

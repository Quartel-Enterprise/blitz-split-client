package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.isEveryoneSettled
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.isRestingScreen
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.repository.ReviewPromptRepository
import com.quare.blitzsplit.core.support.domain.usecase.RequestReviewIfNeeded
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/*
 * Why: the store's review card, asked for as Bible Planner does: only after something went well (a payment that
 * leaves a group with nobody owing, or the third bill created on this device), never in the first days (the first
 * such moment only starts the count), once per app version and at most every two months, since the stores show it a
 * few times a year at most. It waits a little for a home tab or a group's screen to be on top, so it never covers a
 * form or a sheet, and gives up otherwise. A browser has no such card.
 */
class RequestReviewIfNeededUseCase(
    private val appVersion: String,
    private val platform: ClientPlatform,
    private val navigator: Navigator,
    private val repository: ReviewPromptRepository,
    private val observeGroupBalances: ObserveGroupBalances,
    private val currentTimeMillis: CurrentTimeMillis,
    private val trackEvent: TrackEvent,
) : RequestReviewIfNeeded {
    private val grace = 3.days
    private val cooldown = 60.days
    private val restingScreenWait = 10.seconds
    private val balancesWait = 5.seconds

    override suspend fun invoke(moment: ReviewMoment): Boolean {
        if (platform == ClientPlatform.WEB || !isWorthAsking(moment)) return false
        val now = currentTimeMillis()
        val state = repository.get()
        val firstEligibleAt = state.firstEligibleAt
        if (firstEligibleAt == null) {
            repository.update { it.copy(firstEligibleAt = now) }
            return false
        }
        val isAllowed = hasElapsed(now, firstEligibleAt, grace) &&
            state.lastPromptedVersion != appVersion &&
            (state.lastPromptedAt == null || hasElapsed(now, state.lastPromptedAt, cooldown))
        if (!isAllowed) return false
        withTimeoutOrNull(restingScreenWait) {
            navigator.backStack.first { it.lastOrNull()?.isRestingScreen() == true }
        } ?: return false
        repository.update {
            it.copy(
                lastPromptedAt = now,
                lastPromptedVersion = appVersion,
            )
        }
        trackEvent("in_app_review_requested", mapOf("trigger" to moment.trigger))
        return true
    }

    private suspend fun isWorthAsking(moment: ReviewMoment): Boolean = when (moment) {
        ReviewMoment.BillCreated ->
            repository.update { it.copy(billsCreated = it.billsCreated + 1) }.billsCreated >= BILLS_BEFORE_ASKING

        // Why: a group that went meanwhile may never answer, and one moment must not hold the ones after it.
        is ReviewMoment.PaymentRecorded -> moment.groupIds.any { groupId ->
            withTimeoutOrNull(balancesWait) { observeGroupBalances(groupId).first() }?.isEveryoneSettled == true
        }
    }

    // Why: a clock set back since then never silences the review for good.
    private fun hasElapsed(
        now: Long,
        since: Long,
        duration: Duration,
    ): Boolean {
        val elapsed = (now - since).milliseconds
        return elapsed < Duration.ZERO || elapsed >= duration
    }

    private companion object {
        const val BILLS_BEFORE_ASKING = 3
    }
}

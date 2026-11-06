package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.profile.domain.usecase.UpdatePushEnabled
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastNotificationPromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastNotificationPromptAt
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.NotificationPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/*
 * Why: Home's notification card (issue 208): the permission each platform reads, Apple's quiet request, the card's
 * answers and their cooldown, and what Analytics counts of it, apart from the rest of Home.
 */
internal class HomeNotificationPrompts(
    private val uiState: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val trackEvent: TrackEvent,
    private val updateLastNotificationPromptAt: UpdateLastNotificationPromptAt,
    private val updatePushEnabled: UpdatePushEnabled,
    private val currentTimeMillis: CurrentTimeMillis,
    private val emit: (HomeUiAction) -> Unit,
) {
    // Why: how long "Turn on" or "Not now" keeps the notification card away (Pierre's call, issue 208).
    private val cooldown = 21.days

    // Why: the card the last "Turn on" came from, until the next check of the permission says how the system answered.
    private var pendingRequest: NotificationPrompt? = null

    /*
     * Why: the quiet request is out, or Apple answered it: Apple never goes back to "never asked", so such a read was
     * started before the request, whenever it lands. Only an answer that changed nothing lets it through again.
     */
    private var ignoresNeverAsked = false

    // Why: the card counted as shown, until it goes away, so a return to Home doesn't count it again.
    private var shownPrompt: NotificationPrompt? = null

    fun watch(
        observeLastNotificationPromptAt: ObserveLastNotificationPromptAt,
        minuteTicks: MinuteTicks,
        isHomeOnTop: Flow<Boolean>,
    ) {
        // Why: each minute too, so a card snoozed three weeks ago comes back without leaving Início.
        combine(observeLastNotificationPromptAt(), minuteTicks()) { lastPromptAt, _ -> lastPromptAt }
            .onEach { lastPromptAt ->
                uiState.update { it.copy(isNotificationPromptSnoozed = isSnoozed(lastPromptAt)) }
            }.launchIn(scope)
        /*
         * Why: once per showing of the card, whatever brought it (a group, the permission read, three weeks gone), and
         * only once Home is on top to draw it, never behind another tab or screen.
         */
        combine(uiState.map { it.notificationPrompt }, isHomeOnTop, ::Pair)
            .distinctUntilChanged()
            .onEach { (prompt, isOnTop) ->
                if (prompt == null) {
                    shownPrompt = null
                } else if (isOnTop && prompt != shownPrompt) {
                    shownPrompt = prompt
                    trackEvent("notification_prompt_shown", mapOf("prompt" to prompt))
                }
            }.launchIn(scope)
        uiState
            .map { it.shouldAskQuietly }
            .distinctUntilChanged()
            .onEach { shouldAsk -> if (shouldAsk) askQuietly() }
            .launchIn(scope)
    }

    // Why: what the platform read on appear, on return or after its prompt.
    fun onChecked(
        permission: NotificationPermission,
        canAskQuietly: Boolean,
    ) {
        if (ignoresNeverAsked && permission == NotificationPermission.NOT_DETERMINED) return
        check(permission, canAskQuietly)
    }

    // Why: Apple's read right after its quiet request: "never asked" here means the request changed nothing.
    fun onQuietAnswered(permission: NotificationPermission) {
        ignoresNeverAsked = permission != NotificationPermission.NOT_DETERMINED
        check(permission, canAskQuietly = true)
    }

    /*
     * Why: also turns Notifications' "Push notifications" switch on for this device (Pierre's call), so the "Notifications
     * on" a grant shows is true even after the switch was turned off.
     */
    fun turnOn() {
        answer { prompt ->
            trackEvent("notification_prompt_turn_on_clicked", mapOf("prompt" to prompt))
            pendingRequest = prompt
            scope.launch { updatePushEnabled(true) }
            emit(
                when (prompt) {
                    // Why: Apple's full prompt still asks while it delivers quietly.
                    NotificationPrompt.SOFT_ASK, NotificationPrompt.ASK_AGAIN, NotificationPrompt.QUIET ->
                        HomeUiAction.RequestNotificationPermission

                    NotificationPrompt.BLOCKED -> HomeUiAction.OpenNotificationSettings
                },
            )
        }
    }

    fun dismiss() {
        answer { prompt -> trackEvent("notification_prompt_dismissed", mapOf("prompt" to prompt)) }
    }

    /*
     * Why: "Notifications on" only after the card's own "Turn on" (the prompt, or back from the settings), so a change
     * made from Notifications, which confirms by its own toggles, never shows it over that screen.
     */
    private fun check(
        permission: NotificationPermission,
        canAskQuietly: Boolean,
    ) {
        uiState.update {
            it.copy(
                notificationPermission = permission,
                canAskQuietly = canAskQuietly,
            )
        }
        // Why: the first check after "Turn on" is the system's answer: its prompt, or back from its settings.
        val prompt = pendingRequest ?: return
        pendingRequest = null
        val isGranted = permission == NotificationPermission.GRANTED
        trackEvent(
            "notification_permission_answered",
            mapOf(
                "source" to "home",
                "prompt" to prompt,
                "is_granted" to isGranted,
            ),
        )
        if (isGranted) emit(HomeUiAction.ShowNotificationsOn)
    }

    // Why: the permission is unknown until Apple answers the request, so neither card shows meanwhile.
    private fun askQuietly() {
        ignoresNeverAsked = true
        uiState.update {
            it.copy(
                hasAskedQuietly = true,
                notificationPermission = null,
            )
        }
        emit(HomeUiAction.RequestQuietNotifications)
    }

    // Why: either answer snoozes the card, at once so a second tap finds none, then for three weeks on this device.
    private fun answer(onAnswered: (NotificationPrompt) -> Unit) {
        val prompt = uiState.value.notificationPrompt ?: return
        uiState.update { it.copy(isNotificationPromptSnoozed = true) }
        scope.launch { updateLastNotificationPromptAt(currentTimeMillis()) }
        onAnswered(prompt)
    }

    // Why: a clock set back since the last answer never silences the card for good.
    private fun isSnoozed(lastPromptAt: Long?): Boolean {
        if (lastPromptAt == null) return false
        val elapsed = (currentTimeMillis() - lastPromptAt).milliseconds
        return elapsed >= Duration.ZERO && elapsed < cooldown
    }
}

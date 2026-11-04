package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.RequestUpdatePromptIfNeeded
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.isHomeTab
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastUpdatePromptAt
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/*
 * Why: each time the app comes to the front, as Bible Planner does: a new version is offered at most once an hour, and
 * one Google Play already downloaded is installed again after 15 minutes, since its install restarts the app and
 * shouldn't keep interrupting. The store is only asked once the shorter of the two has passed, and the offer waits
 * until a home tab or a group's screen is on top, so it never covers a form, a sheet or a cover whose typing a new top
 * would lose.
 */
class RequestUpdatePromptIfNeededUseCase(
    private val mechanism: UpdateMechanism,
    private val navigator: Navigator,
    private val checkForUpdate: CheckForUpdate,
    private val observeLastUpdatePromptAt: ObserveLastUpdatePromptAt,
    private val currentTimeMillis: CurrentTimeMillis,
    private val showUpdatePrompt: ShowUpdatePrompt,
) : RequestUpdatePromptIfNeeded {
    private val availablePromptCooldown = 1.hours
    private val downloadedInstallCooldown = 15.minutes

    override suspend fun invoke() {
        if (mechanism == UpdateMechanism.NONE) return
        val lastPromptAt = observeLastUpdatePromptAt().first()
        if (!hasCooldownElapsed(lastPromptAt, downloadedInstallCooldown)) return
        val availability = checkForUpdate()
        if (availability !is UpdateAvailability.Pending) return
        if (hasCooldownElapsed(lastPromptAt, availability.cooldown)) {
            navigator.backStack.first { it.lastOrNull()?.isRestingScreen() == true }
            showUpdatePrompt(
                availability = availability,
                source = UpdatePromptSource.STARTUP,
            )
        }
    }

    // Why: a clock set back since the last prompt never silences it for good.
    private fun hasCooldownElapsed(
        lastPromptAt: Long?,
        cooldown: Duration,
    ): Boolean {
        if (lastPromptAt == null) return true
        val elapsed = (currentTimeMillis() - lastPromptAt).milliseconds
        return elapsed < Duration.ZERO || elapsed >= cooldown
    }

    private fun Route.isRestingScreen(): Boolean = isHomeTab() || this is Route.GroupDetail

    private val UpdateAvailability.Pending.cooldown: Duration
        get() = when (this) {
            is UpdateAvailability.Available -> availablePromptCooldown
            UpdateAvailability.Downloaded -> downloadedInstallCooldown
        }
}

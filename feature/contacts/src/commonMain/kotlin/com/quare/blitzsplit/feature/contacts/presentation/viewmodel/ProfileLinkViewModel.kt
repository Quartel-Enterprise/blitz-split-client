package com.quare.blitzsplit.feature.contacts.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfileShareLink
import com.quare.blitzsplit.core.profile.domain.usecase.ResolveProfileLink
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkStage
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: a profile link opened, blitzsplit.com/u/<code>. It shows what the person already has of the link's owner, so
 * nothing is drawn twice: the person's own link opens Profile, and a contact's (someone they share a group with) their
 * contact profile, with the groups in common and the balance, in place of this page. Anyone else gets the name and the
 * photo the server answers, with the invite to a group. Only the person's own link opens offline, from the code kept
 * on the device; any other asks the server, signed out too.
 */
class ProfileLinkViewModel(
    private val route: Route.ProfileLink,
    private val navigator: Navigator,
    private val resolveProfileLink: ResolveProfileLink,
    private val observeProfileShareLink: ObserveProfileShareLink,
    private val observeAccountStatus: ObserveAccountStatus,
    private val observeContactBook: ObserveContactBook,
    private val observeProfile: ObserveProfile,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<ProfileLinkUiEvent>(trackEvent) {
    private val sourceParams: Map<String, Any> = mapOf("source" to "profile_link")

    val uiState: StateFlow<ProfileLinkUiState>
        field = MutableStateFlow(ProfileLinkUiState.LOADING)

    val stateWatcher: FlowWatcher<ProfileLinkUiState> = FlowWatcher(uiState)

    private var resolving: Job? = null

    init {
        resolve()
        // Why: a link that failed offline opens by itself once the device is back online.
        connectivityObserver
            .observe()
            .distinctUntilChanged()
            .drop(1)
            .filter { isOnline -> isOnline && uiState.value.stage == ProfileLinkStage.UNREACHABLE }
            .onEach { resolve() }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ProfileLinkUiEvent) {
        when (event) {
            ProfileLinkUiEvent.OnBackClicked -> if (isOnTop()) navigator.navigateBack()

            ProfileLinkUiEvent.OnRetryClicked -> if (uiState.value.stage == ProfileLinkStage.UNREACHABLE) {
                trackEvent("retry_clicked", sourceParams)
                resolve()
            }

            ProfileLinkUiEvent.OnInviteClicked -> {
                val state = uiState.value
                if (state.stage == ProfileLinkStage.STRANGER && state.canInvite && isOnTop()) {
                    trackEvent("profile_link_invite_clicked", emptyMap())
                    navigator.navigate(
                        Route.ContactInvite(
                            personKey = null,
                            name = state.name,
                        ),
                    )
                }
            }
        }
    }

    private fun resolve() {
        if (resolving?.isActive == true) return
        resolving = viewModelScope.launch {
            val ownCode = observeProfileShareLink().first()?.let(ProfileLink::codeOf)
            if (ownCode == route.code) return@launch openInstead(Route.Profile)
            uiState.value = ProfileLinkUiState.LOADING
            resolveProfileLink(route.code).fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        uiState.value = ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.NOT_FOUND)
                    } else {
                        show(profile)
                    }
                },
                onFailure = {
                    uiState.value = ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.UNREACHABLE)
                },
            )
        }
    }

    // Why: the server only names the account to the person themself and to whoever shares a group with them.
    private suspend fun show(profile: SharedProfile) {
        val userId = profile.userId
        if (userId != null) {
            val myUserId = when (val status = observeAccountStatus().first()) {
                is AccountStatus.Anonymous -> status.userId
                is AccountStatus.Connected -> status.userId
                AccountStatus.LocalOnly -> null
            }
            if (userId == myUserId) return openInstead(Route.Profile)
            val key = PeopleBalances.toUserKey(userId)
            if (observeContactBook().first().findOrNull(key) != null) return openInstead(Route.ContactProfile(key))
        }
        uiState.value = ProfileLinkUiState(
            stage = ProfileLinkStage.STRANGER,
            name = profile.displayName,
            initials = Initials.of(profile.displayName),
            avatarColor = profile.avatarColor,
            photoUrl = profile.photoUrl,
            canInvite = observeProfile().first() != null,
        )
    }

    // Why: only while the page is still on top, so an answer that comes late never replaces where the person went.
    private fun openInstead(destination: Route) {
        if (isOnTop()) navigator.replaceTop(destination)
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route
}

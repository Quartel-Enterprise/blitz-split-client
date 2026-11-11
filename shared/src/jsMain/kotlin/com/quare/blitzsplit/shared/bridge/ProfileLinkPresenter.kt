@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkStage
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ProfileLinkViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: a profile link's page, blitzsplit.com/u/<code>, while it asks the server or has no contact to open.
class JsProfileLinkState(
    // Why: "loading", "unreachable", "not-found" or "stranger".
    val stage: String,
    val name: String?,
    val initials: String?,
    val avatarColor: String?,
    val photoUrl: String?,
    val canInvite: Boolean,
)

class ProfileLinkPresenter(
    route: JsRoute,
) {
    private val profileLinkRoute = route.route as Route.ProfileLink
    private val holder = ViewModelHolder(ProfileLinkViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(profileLinkRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsProfileLinkState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsProfileLinkState(
                stage = toJsProfileLinkStage(state.stage),
                name = state.name,
                initials = state.initials,
                avatarColor = state.avatarColor?.code,
                photoUrl = state.photoUrl,
                canInvite = state.canInvite,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(ProfileLinkUiEvent.OnBackClicked)
    }

    fun retry() {
        viewModel.onEvent(ProfileLinkUiEvent.OnRetryClicked)
    }

    fun invite() {
        viewModel.onEvent(ProfileLinkUiEvent.OnInviteClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsProfileLinkStage(stage: ProfileLinkStage): String = when (stage) {
    ProfileLinkStage.LOADING -> "loading"
    ProfileLinkStage.UNREACHABLE -> "unreachable"
    ProfileLinkStage.NOT_FOUND -> "not-found"
    ProfileLinkStage.STRANGER -> "stranger"
}

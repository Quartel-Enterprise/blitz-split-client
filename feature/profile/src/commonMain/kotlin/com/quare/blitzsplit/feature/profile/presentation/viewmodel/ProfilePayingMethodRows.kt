package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

// Why: Profile's "Payment methods": how the person pays, private to them, apart from the keys they receive with.
internal class ProfilePayingMethodRows(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val navigator: Navigator,
    private val trackEvent: TrackEvent,
) {
    // Why: a restored account's methods only come with its first full sync, so never "none" before it.
    fun follow(
        methods: Flow<List<PayingMethod>>,
        firstDownload: Flow<FirstDownloadProgress?>,
        scope: CoroutineScope,
    ) {
        combine(methods, firstDownload) { list, download -> if (download != null) Loadable.Loading else list.toLoaded() }
            .onEach { loadable -> uiState.update { it.copy(payingMethods = loadable) } }
            .launchIn(scope)
    }

    fun onEvent(event: ProfileUiEvent.PayingMethods) {
        when (event) {
            ProfileUiEvent.PayingMethods.OnAddClicked -> {
                trackEvent("paying_method_add_clicked", emptyMap())
                open(methodId = null)
            }

            is ProfileUiEvent.PayingMethods.OnClicked -> {
                val method = uiState.value.payingMethods
                    .valueOrNull()
                    ?.find { it.id == event.methodId } ?: return
                trackEvent("paying_method_clicked", mapOf("type" to method.type.code))
                open(method.id)
            }
        }
    }

    // Why: never twice, so a double tap doesn't stack the form over itself.
    private fun open(methodId: String?) {
        if (navigator.backStack.value.lastOrNull() is Route.PayingMethod) return
        navigator.navigate(Route.PayingMethod(methodId))
    }
}

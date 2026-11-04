package com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateUiAction
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateUiEvent
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * Why: "Atualização disponível" (iOS and macOS), as Bible Planner's: the App Store's newer version, "Abrir na App
 * Store", which opens the store and closes the sheet, and "Agora não". Opening the app again offers it at most once an
 * hour.
 */
class InAppUpdateViewModel(
    private val route: Route.InAppUpdate,
    private val navigator: Navigator,
    private val trackEvent: TrackEvent,
) : ViewModel() {
    val uiState: StateFlow<InAppUpdateUiState>
        field = MutableStateFlow(InAppUpdateUiState(versionName = route.versionName))

    val uiAction: SharedFlow<InAppUpdateUiAction>
        field = MutableSharedFlow<InAppUpdateUiAction>()

    val stateWatcher: FlowWatcher<InAppUpdateUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<InAppUpdateUiAction> = FlowWatcher(uiAction)

    init {
        trackEvent(
            "update_prompt_shown",
            buildMap {
                put("source", route.source)
                route.versionName?.let { put("version", it) }
            },
        )
    }

    fun onEvent(event: InAppUpdateUiEvent) {
        when (event) {
            InAppUpdateUiEvent.OnUpdateClicked -> openStore()
            InAppUpdateUiEvent.OnDismissed -> dismiss()
        }
    }

    private fun openStore() {
        trackEvent("update_accepted", mapOf("source" to route.source))
        viewModelScope.launch {
            uiAction.emit(InAppUpdateUiAction.OpenStore(route.storeUrl))
            closeSheet()
        }
    }

    private fun dismiss() {
        trackEvent("update_dismissed", mapOf("source" to route.source))
        closeSheet()
    }

    // Why: the sheet's own route only, so a dismiss arriving after the store's tap never pops what is under it.
    private fun closeSheet() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}

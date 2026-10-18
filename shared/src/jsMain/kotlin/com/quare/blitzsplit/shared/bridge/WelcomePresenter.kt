@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsWelcomeState(
    val areAccountButtonsEnabled: Boolean,
    val showsOfflineNotice: Boolean,
    val connectingProvider: String?,
    val hasSignInFailed: Boolean,
)

class WelcomePresenter {
    private val holder = ViewModelHolder(WelcomeViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsWelcomeState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsWelcomeState(
                areAccountButtonsEnabled = state.areAccountButtonsEnabled,
                showsOfflineNotice = state.showsOfflineNotice,
                connectingProvider = state.connectingProvider?.code,
                hasSignInFailed = state.error != null,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun clickProvider(providerCode: String) {
        viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(providerCode.toAccountProvider()))
    }

    fun useWithoutAccount() {
        viewModel.onEvent(WelcomeUiEvent.OnUseWithoutAccountClicked)
    }

    fun usedOnAnotherDevice() {
        viewModel.onEvent(WelcomeUiEvent.OnUsedOnAnotherDeviceClicked)
    }

    fun dismissError() {
        viewModel.onEvent(WelcomeUiEvent.OnErrorDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

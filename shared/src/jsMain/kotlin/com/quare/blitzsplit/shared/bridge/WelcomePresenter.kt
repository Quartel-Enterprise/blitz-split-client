@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

    fun termsOfService() {
        viewModel.onEvent(WelcomeUiEvent.OnTermsClicked)
    }

    fun privacyPolicy() {
        viewModel.onEvent(WelcomeUiEvent.OnPrivacyPolicyClicked)
    }

    // Why: the site's legal page to open in another tab; the native sign in actions never reach a browser.
    fun observeOpenUrls(onUrl: (String) -> Unit): JsWatch {
        val urls = viewModel.uiAction.mapNotNull { action -> (action as? WelcomeUiAction.OpenUrl)?.url }
        val watch = FlowWatcher(urls).observe(onUrl)
        return JsWatch(watch::cancel)
    }

    fun dismissError() {
        viewModel.onEvent(WelcomeUiEvent.OnErrorDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

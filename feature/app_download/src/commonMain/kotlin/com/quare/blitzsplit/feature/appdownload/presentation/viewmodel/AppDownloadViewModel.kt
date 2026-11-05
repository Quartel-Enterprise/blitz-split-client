package com.quare.blitzsplit.feature.appdownload.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.DismissAppInvite
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiAction
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiEvent
import com.quare.blitzsplit.feature.appdownload.presentation.model.AppDownloadUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: the app's code (14.2), a dialog on the web and a sheet on the Mac, opened from Home's card and Profile's row.
 * It reads the same invite as they do, so "I downloaded it" here takes the card and the highlight away too.
 */
class AppDownloadViewModel(
    private val route: Route.AppDownload,
    private val navigator: Navigator,
    private val dismissAppInvite: DismissAppInvite,
    encodeQrCode: EncodeQrCode,
    observeAppInvite: ObserveAppInvite,
    observeAccount: ObserveAccount,
    trackEvent: TrackEvent,
) : TrackedViewModel<AppDownloadUiEvent>(trackEvent) {
    val uiState: StateFlow<AppDownloadUiState>
        field = MutableStateFlow(
            AppDownloadUiState(
                isLoaded = false,
                link = AppInvite.WEB_LINK,
                qrCode = null,
                stores = StoreListing.BOTH,
                phoneUseSinceMillis = null,
                accountProvider = null,
                isDismissed = false,
            ),
        )

    val uiAction: SharedFlow<AppDownloadUiAction>
        field = MutableSharedFlow<AppDownloadUiAction>()

    val stateWatcher: FlowWatcher<AppDownloadUiState> = FlowWatcher(uiState)

    private var isDismissing = false
    val actionWatcher: FlowWatcher<AppDownloadUiAction> = FlowWatcher(uiAction)

    init {
        combine(observeAppInvite(), observeAccount(), ::Pair)
            .onEach { (invite, account) ->
                // Why: nothing to download here (a phone, or no store lists the app any longer).
                if (invite == null) {
                    close()
                    return@onEach
                }
                // Why: encoded before the update, whose block may run again, and only this collector writes the code.
                val current = uiState.value
                val qrCode = current.qrCode.takeIf { invite.link == current.link } ?: encodeQrCode(invite.link)
                uiState.update { state ->
                    state.copy(
                        isLoaded = true,
                        link = invite.link,
                        qrCode = qrCode,
                        stores = invite.stores,
                        phoneUseSinceMillis = invite.phoneUse?.sinceMillis,
                        accountProvider = (account.status as? AccountStatus.Connected)?.provider,
                        isDismissed = invite.isDismissed,
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: AppDownloadUiEvent) {
        when (event) {
            AppDownloadUiEvent.OnCloseClicked -> close()

            // Why: once, also for a second click before Room says the invite went.
            AppDownloadUiEvent.OnDownloadedClicked -> if (uiState.value.showsDownloadedButton && !isDismissing) {
                isDismissing = true
                trackEvent("app_invite_downloaded_clicked", mapOf("source" to "app_download"))
                viewModelScope.launch {
                    dismissAppInvite()
                    close()
                }
            }

            AppDownloadUiEvent.OnCopyLinkClicked -> viewModelScope.launch {
                uiAction.emit(AppDownloadUiAction.CopyLink(uiState.value.link))
            }
        }
    }

    // Why: the code's own route only, so a late close never pops what is under it.
    private fun close() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}

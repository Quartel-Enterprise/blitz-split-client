package com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.support.domain.model.SupportContact
import com.quare.blitzsplit.core.support.domain.usecase.BuildSupportEmailLink
import com.quare.blitzsplit.core.support.domain.usecase.ObserveSupportDiagnostics
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportNotice
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiAction
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiEvent
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded
import software.quare.loadable.valueOrNull

class ContactSupportViewModel(
    private val navigator: Navigator,
    private val buildSupportEmailLink: BuildSupportEmailLink,
    observeSupportDiagnostics: ObserveSupportDiagnostics,
    trackEvent: TrackEvent,
) : TrackedViewModel<ContactSupportUiEvent>(trackEvent) {
    val uiState: StateFlow<ContactSupportUiState>
        field = MutableStateFlow(
            ContactSupportUiState(
                email = SupportContact.EMAIL,
                diagnostics = Loadable.Loading,
                notice = null,
            ),
        )

    val uiAction: SharedFlow<ContactSupportUiAction>
        field = MutableSharedFlow<ContactSupportUiAction>()

    val stateWatcher: FlowWatcher<ContactSupportUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<ContactSupportUiAction> = FlowWatcher(uiAction)

    init {
        observeSupportDiagnostics()
            .onEach { diagnostics -> uiState.update { it.copy(diagnostics = diagnostics.toLoaded()) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ContactSupportUiEvent) {
        when (event) {
            ContactSupportUiEvent.OnDismissed -> dismiss()

            ContactSupportUiEvent.OnOpenEmailClicked -> openEmail()

            ContactSupportUiEvent.OnCopyEmailClicked -> copyEmail(ContactSupportNotice.EMAIL_COPIED)

            // Why: without an email app the address is still what the person needs, so it goes to the clipboard.
            ContactSupportUiEvent.OnOpenEmailFailed -> copyEmail(ContactSupportNotice.NO_EMAIL_APP)
        }
    }

    private fun dismiss() {
        if (navigator.backStack.value.lastOrNull() != Route.ContactSupport) return
        trackEvent("contact_support_dismissed", emptyMap())
        navigator.navigateBack()
    }

    private fun openEmail() {
        val diagnostics = uiState.value.diagnostics.valueOrNull() ?: return
        trackEvent("contact_support_open_email_clicked", emptyMap())
        // Why: another try starts clean, so the notice of the last one never outlives an email app that now opens.
        uiState.update { it.copy(notice = null) }
        viewModelScope.launch { uiAction.emit(ContactSupportUiAction.OpenEmail(buildSupportEmailLink(diagnostics))) }
    }

    private fun copyEmail(notice: ContactSupportNotice) {
        uiState.update { it.copy(notice = notice) }
        viewModelScope.launch { uiAction.emit(ContactSupportUiAction.CopyToClipboard(uiState.value.email)) }
    }
}

@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiAction
import com.quare.blitzsplit.feature.contactsupport.presentation.model.ContactSupportUiEvent
import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

class JsContactSupportState(
    val email: String,
    // Why: what the email carries shimmers until it was read; the fields under it are null until then.
    val isDiagnosticsLoading: Boolean,
    val appVersion: String?,
    // Why: "android", "ios", "macos" or "web".
    val platform: String?,
    // Why: the language's tag ("pt-BR"), null while it follows the browser's.
    val languageTag: String?,
    // Why: the language the app speaks ("pt"), shown next to "System" while it follows the browser's.
    val spokenLanguage: String?,
    // Why: "local_only", "anonymous", "google" or "apple".
    val account: String?,
    val profileLink: String?,
    // Why: "email_copied" or "no_email_app", null before any tap.
    val notice: String?,
    val canOpenEmail: Boolean,
)

class JsContactSupportAction(
    // Why: "open" with the mailto link, or "copy" with the address.
    val type: String,
    val value: String,
)

class ContactSupportPresenter {
    private val holder = ViewModelHolder(ContactSupportViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsContactSupportState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            val diagnostics = state.diagnostics.valueOrNull()
            JsContactSupportState(
                email = state.email,
                isDiagnosticsLoading = state.diagnostics.isLoading(),
                appVersion = diagnostics?.appVersion,
                platform = diagnostics?.platform?.name?.lowercase(),
                languageTag = diagnostics?.language?.tag,
                spokenLanguage = diagnostics?.spokenLanguage,
                account = diagnostics?.account?.name?.lowercase(),
                profileLink = diagnostics?.profileLink,
                notice = state.notice?.name?.lowercase(),
                canOpenEmail = state.canOpenEmail,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsContactSupportAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is ContactSupportUiAction.OpenEmail -> JsContactSupportAction("open", action.message.mailtoLink)
                is ContactSupportUiAction.CopyToClipboard -> JsContactSupportAction("copy", action.email)
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun dismiss() {
        viewModel.onEvent(ContactSupportUiEvent.OnDismissed)
    }

    fun openEmail() {
        viewModel.onEvent(ContactSupportUiEvent.OnOpenEmailClicked)
    }

    fun copyEmail() {
        viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailClicked)
    }

    // Why: the browser refused the clipboard.
    fun copyEmailFailed() {
        viewModel.onEvent(ContactSupportUiEvent.OnCopyEmailFailed)
    }

    fun dispose() {
        holder.clear()
    }
}

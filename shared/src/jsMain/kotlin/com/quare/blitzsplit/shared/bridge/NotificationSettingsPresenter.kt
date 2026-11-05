@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.mp.KoinPlatform

class JsNotificationToggle(
    // Why: the NotificationChannel's name, "PUSH" or "IN_APP", which picks its column and goes back with a toggle.
    val channel: String,
    val isOn: Boolean,
    val isEnabled: Boolean,
)

class JsNotificationItem(
    // Why: the NotificationType's name, which picks the title and subtitle in each language.
    val type: String,
    // Why: one per column, Push then In app; null where the row has no such toggle (the weekly reminder's In app).
    val toggles: Array<JsNotificationToggle?>,
)

class JsNotificationSettingsState(
    val items: Array<JsNotificationItem>,
    val showsPermissionDenied: Boolean,
    // Why: the toggles shimmer while the choices load or the browser's permission wasn't reported yet.
    val showsTogglesLoading: Boolean,
)

class JsNotificationSettingsAction(
    // Why: "request-permission" or "open-settings".
    val type: String,
)

class NotificationSettingsPresenter {
    private val holder = ViewModelHolder(NotificationSettingsViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsNotificationSettingsState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsNotificationSettingsState(
                items = state.types
                    .map { type ->
                        JsNotificationItem(
                            type = type.name,
                            toggles = state.channels
                                .map { channel ->
                                    JsNotificationToggle(
                                        channel = channel.name,
                                        isOn = state.isEnabled(type, channel),
                                        isEnabled = state.isToggleEnabled(type, channel),
                                    ).takeIf { state.hasToggle(type, channel) }
                                }.toTypedArray(),
                        )
                    }.toTypedArray(),
                showsPermissionDenied = state.showsPermissionDenied,
                showsTogglesLoading = state.showsTogglesLoading,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsNotificationSettingsAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.mapNotNull { action ->
            when (action) {
                NotificationSettingsUiAction.RequestPermission -> JsNotificationSettingsAction("request-permission")

                NotificationSettingsUiAction.OpenSystemSettings -> JsNotificationSettingsAction("open-settings")

                // Why: Android's channels only; the web never reports a row blocked.
                is NotificationSettingsUiAction.OpenChannelSettings -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    // Why: the NotificationPermission's name, read from the browser's Notification.permission.
    fun permissionChecked(permissionName: String) {
        val permission = NotificationPermission.entries.firstOrNull { it.name == permissionName } ?: return
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(permission))
    }

    // Why: the same, read right after the browser's prompt the app asked for, which answers it.
    fun permissionAnswered(permissionName: String) {
        val permission = NotificationPermission.entries.firstOrNull { it.name == permissionName } ?: return
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionAnswered(permission))
    }

    // Why: the NotificationType's and the NotificationChannel's names ("PUSH" or "IN_APP").
    fun toggle(
        typeName: String,
        channelName: String,
        isEnabled: Boolean,
    ) {
        val type = NotificationType.entries.firstOrNull { it.name == typeName } ?: return
        val channel = NotificationChannel.entries.firstOrNull { it.name == channelName } ?: return
        viewModel.onEvent(NotificationSettingsUiEvent.OnToggled(type, channel, isEnabled))
    }

    // Why: the dialog Activity opens closes back to it; inside the profile nothing is left to close.
    fun back() {
        viewModel.onEvent(NotificationSettingsUiEvent.OnBackClicked)
    }

    fun openSettings() {
        viewModel.onEvent(NotificationSettingsUiEvent.OnOpenSettingsClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

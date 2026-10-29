@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsNotificationItem(
    // Why: the NotificationType's name, which picks the title and subtitle in each language.
    val type: String,
    val isEnabled: Boolean,
)

class JsNotificationSettingsState(
    val items: Array<JsNotificationItem>,
    val showsPermissionDenied: Boolean,
    val areTogglesEnabled: Boolean,
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
                            isEnabled = state.isEnabled(type),
                        )
                    }.toTypedArray(),
                showsPermissionDenied = state.showsPermissionDenied,
                areTogglesEnabled = state.areTogglesEnabled,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsNotificationSettingsAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                NotificationSettingsUiAction.RequestPermission -> JsNotificationSettingsAction("request-permission")
                NotificationSettingsUiAction.OpenSystemSettings -> JsNotificationSettingsAction("open-settings")
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

    fun toggle(
        typeName: String,
        isEnabled: Boolean,
    ) {
        val type = NotificationType.entries.firstOrNull { it.name == typeName } ?: return
        viewModel.onEvent(NotificationSettingsUiEvent.OnToggled(type, isEnabled))
    }

    fun openSettings() {
        viewModel.onEvent(NotificationSettingsUiEvent.OnOpenSettingsClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

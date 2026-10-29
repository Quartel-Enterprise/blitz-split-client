package com.quare.blitzsplit.android.feature.notificationsettings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.SystemNotificationPermission
import com.quare.blitzsplit.android.platform.rememberNotificationPermissionAccess
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionAccess = rememberNotificationPermissionAccess { permission ->
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(permission.toNotificationPermission()))
    }
    // Why: on every return too, since the person may have changed it in the system settings.
    LifecycleResumeEffect(viewModel, permissionAccess) {
        viewModel.onEvent(
            NotificationSettingsUiEvent.OnPermissionChecked(permissionAccess.read().toNotificationPermission()),
        )
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel, permissionAccess) {
        viewModel.uiAction.collect { action ->
            when (action) {
                NotificationSettingsUiAction.RequestPermission -> permissionAccess.request()
                NotificationSettingsUiAction.OpenSystemSettings -> permissionAccess.openSettings()
            }
        }
    }
    BackHandler { viewModel.onEvent(NotificationSettingsUiEvent.OnBackClicked) }
    NotificationSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

private fun SystemNotificationPermission.toNotificationPermission(): NotificationPermission = when (this) {
    SystemNotificationPermission.GRANTED -> NotificationPermission.GRANTED
    SystemNotificationPermission.DENIED -> NotificationPermission.DENIED
    SystemNotificationPermission.ASKABLE -> NotificationPermission.NOT_DETERMINED
}

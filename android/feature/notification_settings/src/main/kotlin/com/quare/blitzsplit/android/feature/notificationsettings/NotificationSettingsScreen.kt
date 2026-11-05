package com.quare.blitzsplit.android.feature.notificationsettings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.findSilencedNotificationChannels
import com.quare.blitzsplit.android.platform.openNotificationChannelSettings
import com.quare.blitzsplit.android.platform.rememberNotificationPermissionAccess
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionAccess = rememberNotificationPermissionAccess { permission ->
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(permission))
    }
    val context = LocalContext.current
    // Why: on every return too, since the person may have changed it, or silenced a channel, in the system settings.
    LifecycleResumeEffect(viewModel, permissionAccess, context) {
        viewModel.onEvent(
            NotificationSettingsUiEvent.OnPermissionChecked(permissionAccess.read()),
        )
        viewModel.onEvent(
            NotificationSettingsUiEvent.OnSystemBlockedTypesChecked(
                PushChannel.typesOf(context.findSilencedNotificationChannels(PushChannel.ids)),
            ),
        )
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel, permissionAccess, context) {
        viewModel.uiAction.collect { action ->
            when (action) {
                NotificationSettingsUiAction.RequestPermission -> permissionAccess.request()

                NotificationSettingsUiAction.OpenSystemSettings -> permissionAccess.openSettings()

                is NotificationSettingsUiAction.OpenChannelSettings ->
                    context.openNotificationChannelSettings(PushChannel.of(action.type).id)
            }
        }
    }
    BackHandler { viewModel.onEvent(NotificationSettingsUiEvent.OnBackClicked) }
    NotificationSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

package com.quare.blitzsplit.android.feature.notificationsettings

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationSettingsScreen(viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        context.markNotificationPermissionAsked()
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(context.readNotificationPermission()))
    }
    // Why: on every return too, since the person may have changed it in the system settings.
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(NotificationSettingsUiEvent.OnPermissionChecked(context.readNotificationPermission()))
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                NotificationSettingsUiAction.RequestPermission -> if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                NotificationSettingsUiAction.OpenSystemSettings -> context.openNotificationSettings()
            }
        }
    }
    BackHandler { viewModel.onEvent(NotificationSettingsUiEvent.OnBackClicked) }
    NotificationSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.platform.rememberNotificationPermissionAccess
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: Início, the root of the back stack. The recovery code sheet comes in as a slot, as on Grupos, since features
 * never see each other. A build without push (no Firebase config) never reads the permission, so its card never shows.
 */
@Composable
fun HomeScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    isPushConfigured: Boolean,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            HomeUiAction.ShowChangeError -> R.string.groups_change_error

            HomeUiAction.ShowNotificationsOn -> R.string.groups_home_notifications_on

            HomeUiAction.RequestNotificationPermission,
            HomeUiAction.OpenNotificationSettings,
            HomeUiAction.RequestQuietNotifications,
            -> null
        }
    }
    if (isPushConfigured) NotificationPermissionEffect(viewModel)
    // Why: "Registrar pagamento", which a shortcut or a face opened over Início, just closed.
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(HomeUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(HomeUiEvent.OnRecordedPaymentNoticeShown) },
    )
    DeletedGroupNoticeEffect(
        notice = state.deletedGroupNotice,
        snackbarHostState = snackbarHostState,
        onShown = { viewModel.onEvent(HomeUiEvent.OnDeletedGroupNoticeShown) },
    )
    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}

// Why: read on every return too, since the person may change it in the system settings, and after the system's dialog.
@Composable
private fun NotificationPermissionEffect(viewModel: HomeViewModel) {
    val permissionAccess = rememberNotificationPermissionAccess { permission ->
        viewModel.onEvent(HomeUiEvent.OnNotificationPermissionChecked(permission, canAskQuietly = false))
    }
    LifecycleResumeEffect(viewModel, permissionAccess) {
        viewModel.onEvent(HomeUiEvent.OnNotificationPermissionChecked(permissionAccess.read(), canAskQuietly = false))
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel, permissionAccess) {
        viewModel.uiAction.collect { action ->
            when (action) {
                HomeUiAction.RequestNotificationPermission -> permissionAccess.request()

                HomeUiAction.OpenNotificationSettings -> permissionAccess.openSettings()

                // Why: Android has no quiet delivery, and never reports it can ask quietly.
                HomeUiAction.ShowChangeError,
                HomeUiAction.ShowNotificationsOn,
                HomeUiAction.RequestQuietNotifications,
                -> Unit
            }
        }
    }
}

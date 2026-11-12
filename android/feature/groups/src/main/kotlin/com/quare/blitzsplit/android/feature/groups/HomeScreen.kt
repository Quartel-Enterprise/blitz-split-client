package com.quare.blitzsplit.android.feature.groups

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.R as DesignR
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.platform.ReceiptCameraTaps
import com.quare.blitzsplit.android.platform.ReceiptSourcePicker
import com.quare.blitzsplit.android.platform.ReceiptSourceRequest
import com.quare.blitzsplit.android.platform.rememberNotificationPermissionAccess
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
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

            HomeUiAction.ShowBillDeleteError -> DesignR.string.designsystem_delete_bill_error

            HomeUiAction.ShowNotificationsOn -> R.string.groups_home_notifications_on

            // Why: the invite to the phone app is the web's and the Mac's; Android never shows it.
            HomeUiAction.RequestNotificationPermission,
            HomeUiAction.OpenNotificationSettings,
            HomeUiAction.RequestQuietNotifications,
            HomeUiAction.ShowUsedOnPhone,
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
    ReceiptSourceComponent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    // Why: a bill's ⋮ "Apagar" on "Últimas despesas" asks what Conta asks, in the same words.
    BillDeleteQuestion(
        deletion = state.billToDelete,
        isDeleting = state.isDeletingBill,
        onEvent = viewModel::onEvent,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}

/*
 * Why: "Ler cupom fiscal" asks where the photo comes from on Início, as Rachar conta's own does; the photo then opens
 * Rachar conta reading it (beta feedback, part 3, item 16).
 */
@Composable
private fun ReceiptSourceComponent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (HomeUiEvent) -> Unit,
) {
    ReceiptSourcePicker(
        request = ReceiptSourceRequest(
            showsChoice = state.showsReceiptSourceChoice,
            shownSource = state.receiptSource,
            onSourcePicked = { onEvent(HomeUiEvent.OnReceiptSourcePicked(it)) },
            onChoiceSkipped = { onEvent(HomeUiEvent.OnReceiptChoiceSkipped(it)) },
            onDismissed = { onEvent(HomeUiEvent.OnReceiptPickerDismissed) },
            onPhotoPicked = { onEvent(HomeUiEvent.OnReceiptPhotoPicked(it)) },
            cameraTaps = ReceiptCameraTaps(
                onTorchClicked = { onEvent(HomeUiEvent.OnReceiptTorchClicked(it)) },
                onAllowClicked = { onEvent(HomeUiEvent.OnReceiptCameraAllowClicked) },
                onSettingsClicked = { onEvent(HomeUiEvent.OnReceiptCameraSettingsClicked) },
            ),
        ),
        snackbarHostState = snackbarHostState,
    )
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
                HomeUiAction.ShowBillDeleteError,
                HomeUiAction.ShowNotificationsOn,
                HomeUiAction.RequestQuietNotifications,
                HomeUiAction.ShowUsedOnPhone,
                -> Unit
            }
        }
    }
}

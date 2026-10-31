package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: features never see each other, so the recovery code sheet comes in as a slot the app fills
 * with :android:feature:recovery_code.
 */
@Composable
fun GroupsScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            GroupsUiAction.ShowComingSoon -> R.string.groups_coming_soon
        }
    }
    // Why: state, not an action, so it waits until the home is drawn; the next deleted group follows once it goes.
    val deletedNotice = state.deletedGroupNotice
    val deletedMessage = deletedNotice?.let { notice ->
        notice.deletedByName?.let { stringResource(R.string.groups_deleted_by, it, notice.groupName) }
            ?: stringResource(R.string.groups_deleted, notice.groupName)
    }
    LaunchedEffect(deletedNotice) {
        if (deletedMessage != null) {
            snackbarHostState.showSnackbar(deletedMessage)
            viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)
        }
    }
    GroupsContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}

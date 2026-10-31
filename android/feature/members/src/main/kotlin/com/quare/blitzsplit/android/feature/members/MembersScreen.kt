package com.quare.blitzsplit.android.feature.members

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MembersScreen(
    route: Route.Members,
    viewModel: MembersViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val changeError = stringResource(R.string.members_change_error)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                MembersUiAction.ShowChangeError -> showReplacingSnackbar(snackbarHostState, changeError)

                is MembersUiAction.CopyLink -> context.copyLink(action.link)

                is MembersUiAction.SharePersonalLink -> context.shareText(
                    resources.getString(
                        R.string.members_personal_share_message,
                        action.groupName,
                        action.memberName,
                        action.link,
                    ),
                )
            }
        }
    }
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(MembersUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(MembersUiEvent.OnRecordedPaymentNoticeShown) },
    )
    BackHandler { viewModel.onEvent(MembersUiEvent.OnBackClicked) }
    MembersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    state.editor?.let { editor ->
        MemberNameDialog(
            editor = editor,
            onEvent = viewModel::onEvent,
        )
    }
    state.memberToRemove?.let { member ->
        RemoveMemberDialog(
            name = member.name,
            onEvent = viewModel::onEvent,
        )
    }
    state.personalInvite?.let { invite ->
        PersonalInviteSheet(
            invite = invite,
            groupName = state.groupName,
            onEvent = viewModel::onEvent,
        )
    }
}

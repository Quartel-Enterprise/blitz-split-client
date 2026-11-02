package com.quare.blitzsplit.android.feature.members

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.GroupTabBody
import com.quare.blitzsplit.android.designsystem.component.GroupTabFloatingButton
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * Why: "Membros", the group's tab drawn under its header: the screen asks for it whatever tab is picked, so the members
 * are already read when the tab is tapped, and draws the rows only while it is. A phone upright floats "Adicionar
 * pessoa" over the list; anywhere else the button closes the list. Its dialogs and sheet open over the group.
 */
@Composable
fun rememberMembersTab(
    route: Route.GroupDetail,
    snackbarHostState: SnackbarHostState,
    // Why: its own key, since the group's ViewModel already takes the route's.
    viewModel: MembersViewModel = koinViewModel(key = "members:$route") { parametersOf(route) },
): GroupTabBody {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val changeError = stringResource(R.string.members_change_error)
    val releaseError = stringResource(R.string.members_release_error)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                MembersUiAction.ShowChangeError -> showReplacingSnackbar(snackbarHostState, changeError)

                MembersUiAction.ShowReleaseError -> showReplacingSnackbar(snackbarHostState, releaseError)

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
    MembersOverlay(
        viewModel = viewModel,
    )
    val usesFab = rememberDeviceLayout() == DeviceLayout.PhonePortrait
    val money = rememberMoneyFormatter(state.currency)
    val addLabel = stringResource(R.string.members_add)
    return GroupTabBody(
        listContent = {
            memberItems(
                state = state,
                money = money,
                showsAddButton = !usesFab,
                onEvent = viewModel::onEvent,
            )
        },
        floatingButton = if (usesFab && state.isLoaded) {
            GroupTabFloatingButton(
                icon = Icons.Outlined.PersonAdd,
                label = addLabel,
                position = FabPosition.Start,
                onClick = { viewModel.onEvent(MembersUiEvent.OnAddClicked) },
            )
        } else {
            null
        },
    )
}

@Composable
private fun MembersOverlay(viewModel: MembersViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    state.memberToRelease?.let { member ->
        ReleaseMemberDialog(
            name = member.name,
            isReleasing = state.isReleasing,
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

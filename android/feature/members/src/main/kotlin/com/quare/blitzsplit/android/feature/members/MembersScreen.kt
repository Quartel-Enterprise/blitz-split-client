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
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    val comingSoon = stringResource(R.string.members_coming_soon)
    val changeError = stringResource(R.string.members_change_error)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                MembersUiAction.ShowComingSoon -> showSnackbar(snackbarHostState, comingSoon)

                MembersUiAction.ShowChangeError -> showSnackbar(snackbarHostState, changeError)

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

private fun CoroutineScope.showSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
) {
    snackbarHostState.currentSnackbarData?.dismiss()
    // Why: on its own, so the next action replaces this snackbar instead of waiting until it hides.
    launch { snackbarHostState.showSnackbar(message) }
}

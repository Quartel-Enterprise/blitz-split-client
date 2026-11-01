package com.quare.blitzsplit.android.feature.invite

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InviteScreen(
    route: Route.Invite,
    viewModel: InviteViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is InviteUiAction.CopyLink -> context.copyLink(action.link)

                is InviteUiAction.ShareLink -> context.shareText(
                    resources.getString(R.string.invite_share_message, action.groupName, action.link),
                )

                InviteUiAction.ShowChangeLinkFailed -> scope.showReplacingSnackbar(
                    snackbarHostState,
                    resources.getString(R.string.invite_change_link_failed),
                )
            }
        }
    }
    BackHandler { viewModel.onEvent(InviteUiEvent.OnCloseClicked) }
    InviteContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

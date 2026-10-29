package com.quare.blitzsplit.android.feature.invite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiAction
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InviteScreen(
    route: Route.Invite,
    // Why: a tablet opens the invite as a dialog over the home, as it did the new group.
    isDialog: Boolean,
    viewModel: InviteViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.invite_coming_soon)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                is InviteUiAction.CopyLink -> context.copyLink(action.link)

                is InviteUiAction.ShareLink -> context.shareLink(
                    link = action.link,
                    groupName = action.groupName,
                )

                InviteUiAction.ShowComingSoon -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    // Why: on its own, so a copy or a share right after isn't held until the snackbar hides.
                    launch { snackbarHostState.showSnackbar(comingSoon) }
                }
            }
        }
    }
    BackHandler { viewModel.onEvent(InviteUiEvent.OnCloseClicked) }
    InviteContent(
        state = state,
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

private fun Context.copyLink(link: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(link, link))
}

// Why: the system's share sheet, so the link goes to whichever app the group talks in.
private fun Context.shareLink(
    link: String,
    groupName: String,
) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_message, groupName, link))
    startActivity(Intent.createChooser(send, null))
}

package com.quare.blitzsplit.android.feature.contacts

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.copyLink
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactProfileUiAction
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * Why: "Perfil do contato" (11.2), over Contatos where the window draws one column and in its pane beside the list
 * elsewhere, where it has no back arrow (back closes the pane) and its own ViewModel per person picked.
 */
@Composable
fun ContactProfileScreen(
    route: Route.ContactProfile,
    isInPane: Boolean = false,
    viewModel: ContactProfileViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            context.runProfileAction(action)?.let { scope.showReplacingSnackbar(snackbarHostState, it) }
        }
    }
    ContactProfileContent(
        state = state,
        isInPane = isInPane,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    if (state.isHideConfirmOpen) {
        state.contact?.let { contact ->
            HideContactDialog(
                contact = contact,
                onEvent = viewModel::onEvent,
            )
        }
    }
    state.addToGroup?.let { draft ->
        state.contact?.let { contact ->
            AddToGroupSheet(
                contact = contact,
                draft = draft,
                onEvent = viewModel::onEvent,
            )
        }
    }
    state.merge?.let { merge ->
        state.contact?.let { contact ->
            MergeGuestSheet(
                contact = contact,
                sheet = merge,
                isOnline = state.isOnline,
                onEvent = viewModel::onEvent,
            )
        }
    }
}

// Why: copies or shares what the action carries, and hands back the snackbar's text, null when there is none.
private fun Context.runProfileAction(action: ContactProfileUiAction): String? = when (action) {
    is ContactProfileUiAction.CopyLink -> {
        copyLink(action.link)
        getString(R.string.contacts_copied)
    }

    is ContactProfileUiAction.ShareLink -> {
        shareText(getString(R.string.contacts_merge_share_message, action.name, action.link))
        null
    }

    is ContactProfileUiAction.ShowAddedToGroup -> getString(R.string.contacts_added, action.name, action.groupName)

    ContactProfileUiAction.ShowComingSoon -> getString(R.string.contacts_coming_soon)

    ContactProfileUiAction.ShowChangeError -> getString(R.string.contacts_change_error)

    ContactProfileUiAction.ShowCancelMergeError -> getString(R.string.contacts_cancel_merge_error)
}

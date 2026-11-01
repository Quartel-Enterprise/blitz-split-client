package com.quare.blitzsplit.android.feature.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DetailPlaceholderPane
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.platform.isContactsPermissionGranted
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import org.koin.compose.viewmodel.koinViewModel

// Why: the home's Contatos tab: everyone the user shares a group with, each with the net of every group.
@Composable
fun ContactsScreen(viewModel: ContactsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            ContactsUiAction.ShowComingSoon -> R.string.contacts_coming_soon
            ContactsUiAction.ShowChangeError -> R.string.contacts_change_error
        }
    }
    /*
     * Why: the window decides whether a tap opens the profile beside the list, which the ViewModel needs for taps.
     * Once Contatos leaves the screen (a profile or the agenda over it) it draws no pane.
     */
    val showsPane = rememberContactsPaneLayout() != null
    DisposableEffect(showsPane) {
        viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(showsPane))
        onDispose { viewModel.onEvent(ContactsUiEvent.OnDetailPaneChanged(isShown = false)) }
    }
    // Why: the search looks in the agenda only with the access the agenda's own screen already got; it never asks.
    val context = LocalContext.current
    LaunchedEffect(state.isSearchOpen) {
        if (state.isSearchOpen && context.isContactsPermissionGranted()) {
            viewModel.onEvent(ContactsUiEvent.OnAgendaLoaded(context.readAgenda()))
        }
    }
    val hasPanePick = showsPane && (state.selectedKey != null || state.isAgendaPicked)
    // Why: back closes the pane's pick before it leaves Contatos, and the search before both.
    BackHandler(enabled = hasPanePick && !state.isSearchOpen) { viewModel.onEvent(ContactsUiEvent.OnPaneBackClicked) }
    BackHandler(enabled = state.isSearchOpen) { viewModel.onEvent(ContactsUiEvent.OnSearchClosed) }
    ContactsContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        detailPane = {
            val key = state.selectedKey
            when {
                key != null -> ContactProfileScreen(
                    route = Route.ContactProfile(key),
                    isInPane = true,
                )

                state.isAgendaPicked -> AgendaScreen(isInPane = true)

                else -> DetailPlaceholderPane()
            }
        },
    )
}

package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.SearchQueryField
import com.quare.blitzsplit.android.platform.ContactsPermissionStatus
import com.quare.blitzsplit.android.platform.rememberContactsPermissionAccess
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiAction
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaUiState
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val columnMaxWidth = 640.dp

/*
 * Why: "Convidar da agenda" (11.3), over Contatos on a phone and in its pane in a wide window. The access is read
 * again each time the screen resumes, so allowing it in the settings shows the contacts on coming back.
 */
@Composable
fun AgendaScreen(
    isInPane: Boolean = false,
    viewModel: AgendaViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    lateinit var refresh: () -> Unit
    val access = rememberContactsPermissionAccess { refresh() }
    refresh = {
        val status = access.status
        viewModel.onEvent(AgendaUiEvent.OnAccessChanged(agendaAccessOf(status)))
        if (status == ContactsPermissionStatus.GRANTED) {
            scope.launch { viewModel.onEvent(AgendaUiEvent.OnContactsLoaded(context.readAgenda())) }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                AgendaUiAction.RequestAccess -> access.request()
                AgendaUiAction.OpenSettings -> access.openSettings()
            }
        }
    }
    AgendaContent(
        state = state,
        isInPane = isInPane,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaContent(
    state: AgendaUiState,
    isInPane: Boolean,
    onEvent: (AgendaUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.contacts_agenda)
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (!isInPane) {
                        IconButton(onClick = { onEvent(AgendaUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.contacts_agenda_back),
                            )
                        }
                    }
                },
                windowInsets = if (isInPane) WindowInsets(0) else TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = if (isInPane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = columnMaxWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                agendaItems(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

private fun LazyListScope.agendaItems(
    state: AgendaUiState,
    onEvent: (AgendaUiEvent) -> Unit,
) {
    val onInviteWithLink = { onEvent(AgendaUiEvent.OnInviteWithLinkClicked) }
    when {
        state.showsAskAccess -> item(key = "ask") {
            AccessCard(
                title = stringResource(R.string.contacts_agenda_ask_title),
                body = stringResource(R.string.contacts_agenda_ask_body),
                action = stringResource(R.string.contacts_agenda_allow),
                onAction = { onEvent(AgendaUiEvent.OnAllowAccessClicked) },
                onInviteWithLink = onInviteWithLink,
            )
        }

        state.showsDenied -> item(key = "denied") {
            AccessCard(
                title = stringResource(R.string.contacts_agenda_denied_title),
                body = stringResource(R.string.contacts_agenda_denied_body),
                action = stringResource(R.string.contacts_agenda_open_settings),
                onAction = { onEvent(AgendaUiEvent.OnOpenSettingsClicked) },
                onInviteWithLink = onInviteWithLink,
            )
        }

        state.showsContacts -> contactItems(
            state = state,
            onEvent = onEvent,
        )
    }
}

private fun LazyListScope.contactItems(
    state: AgendaUiState,
    onEvent: (AgendaUiEvent) -> Unit,
) {
    item(key = "search") {
        AgendaSearchField(
            query = state.query,
            onQueryChange = { onEvent(AgendaUiEvent.OnQueryChanged(it)) },
        )
    }
    if (state.showsNoResults) {
        item(key = "no-results") {
            NoResultsMessage(
                title = stringResource(R.string.contacts_no_results_title, state.query.trim()),
                body = stringResource(R.string.contacts_agenda_no_results_body),
                onInvite = { onEvent(AgendaUiEvent.OnInviteWithLinkClicked) },
            )
        }
        return
    }
    item(key = "link") {
        InviteActionRow(
            icon = Icons.Outlined.QrCode2,
            title = stringResource(R.string.contacts_invite_link),
            subtitle = stringResource(R.string.contacts_invite_link_subtitle),
            isSelected = false,
            index = 0,
            count = 1,
            onClick = { onEvent(AgendaUiEvent.OnInviteWithLinkClicked) },
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        )
    }
    if (state.contacts.isEmpty()) {
        item(key = "empty") {
            Text(
                text = stringResource(R.string.contacts_agenda_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    agendaResultItems(
        results = state.results,
        keyPrefix = "agenda",
        onInvite = { onEvent(AgendaUiEvent.OnInviteClicked(it)) },
    )
}

@Composable
private fun AgendaSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(
                    start = 16.dp,
                    end = 4.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SearchQueryField(
                query = query,
                placeholder = stringResource(R.string.contacts_agenda_search),
                onQueryChange = onQueryChange,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.contacts_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Why: "Chame gente da sua agenda" before the system asks, and "Sem acesso à agenda" once refused, both with the link.
@Composable
private fun AccessCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    onInviteWithLink: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                modifier = Modifier.padding(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAction,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
        ) {
            Text(action)
        }
        OutlinedButton(
            onClick = onInviteWithLink,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.contacts_invite_link))
        }
    }
}

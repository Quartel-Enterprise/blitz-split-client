package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.SearchQueryField
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaMatch
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiState

private val rowPadding = Modifier.padding(
    horizontal = 16.dp,
    vertical = 1.dp,
)

/*
 * Why: "Buscar contatos" takes the top bar's place: back, what is typed and ✕. It takes the focus as it opens, so
 * the keyboard comes up at once.
 */
@Composable
internal fun ContactsSearchBar(
    query: String,
    onEvent: (ContactsUiEvent) -> Unit,
    maxWidth: Dp,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopStart,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onEvent(ContactsUiEvent.OnSearchClosed) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.contacts_search_back),
                    )
                }
                SearchQueryField(
                    query = query,
                    placeholder = stringResource(R.string.contacts_search),
                    onQueryChange = { onEvent(ContactsUiEvent.OnQueryChanged(it)) },
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onEvent(ContactsUiEvent.OnQueryChanged("")) }) {
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
}

/*
 * Why: what the search found once something is typed: "Nos seus grupos", then "Na agenda" (when the agenda may be
 * read), or "Ninguém com “Rodrigo”" with the link.
 */
internal fun LazyListScope.searchResultItems(
    state: ContactsUiState,
    isPaneShown: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    if (state.showsNoResults) {
        item(key = "no-results") {
            NoResultsMessage(
                title = stringResource(R.string.contacts_no_results_title, state.query.trim()),
                body = stringResource(R.string.contacts_no_results_body),
                onInvite = { onEvent(ContactsUiEvent.OnInviteWithLinkClicked) },
            )
        }
        return
    }
    val groupResults = state.groupResults
    if (groupResults.isNotEmpty()) {
        item(key = "groups-title") {
            SectionTitle(stringResource(R.string.contacts_search_in_groups, groupResults.size))
        }
        itemsIndexed(groupResults, key = { _, row -> "found-${row.key}" }) { index, row ->
            ContactListRow(
                row = row,
                isSelected = isPaneShown && row.key == state.selectedKey,
                index = index,
                count = groupResults.size,
                onClick = { onEvent(ContactsUiEvent.OnContactClicked(row.key)) },
                modifier = rowPadding,
            )
        }
    }
    agendaResultItems(
        results = state.agendaResults,
        keyPrefix = "found-agenda",
        onInvite = { onEvent(ContactsUiEvent.OnAgendaContactInviteClicked(it)) },
    )
}

// Why: "Na agenda · 148", each with "Convidar", in the search and in "Convidar da agenda".
internal fun LazyListScope.agendaResultItems(
    results: List<AgendaMatch>,
    keyPrefix: String,
    onInvite: (contactId: String) -> Unit,
) {
    if (results.isEmpty()) return
    item(key = "$keyPrefix-title") {
        SectionTitle(stringResource(R.string.contacts_search_in_agenda, results.size))
    }
    itemsIndexed(results, key = { _, match -> "$keyPrefix-${match.contact.id}" }) { index, match ->
        AgendaContactRow(
            match = match,
            index = index,
            count = results.size,
            onInvite = { onInvite(match.contact.id) },
            modifier = rowPadding,
        )
    }
}

// Why: "Ninguém com “Rodrigo”": what was looked for, and the link, which needs nobody to be found.
@Composable
internal fun NoResultsMessage(
    title: String,
    body: String,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp,
            ).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        Button(onClick = onInvite) {
            Text(stringResource(R.string.contacts_invite_link))
        }
    }
}

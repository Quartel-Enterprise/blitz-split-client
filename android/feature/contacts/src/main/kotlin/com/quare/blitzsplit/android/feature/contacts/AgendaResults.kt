package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.contacts.presentation.model.AgendaMatch

private val rowPadding = Modifier.padding(
    horizontal = 16.dp,
    vertical = 1.dp,
)

// Why: "Na agenda · 148", each with "Convidar", in "Convidar da agenda".
internal fun LazyListScope.agendaResultItems(
    results: List<AgendaMatch>,
    keyPrefix: String,
    onInvite: (contactId: String) -> Unit,
) {
    if (results.isEmpty()) return
    item(
        key = "$keyPrefix-title",
        contentType = "title",
    ) {
        SectionTitle(stringResource(R.string.contacts_search_in_agenda, results.size))
    }
    itemsIndexed(
        items = results,
        key = { _, match -> "$keyPrefix-${match.contact.id}" },
        contentType = { _, _ -> "contact" },
    ) { index, match ->
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

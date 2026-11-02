package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapSearchResult

@Composable
internal fun MapSearchField(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    TextField(
        value = state.query,
        onValueChange = { onEvent(CategoryMapUiEvent.OnQueryChanged(it)) },
        placeholder = { Text(stringResource(R.string.category_map_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// Why: "Offline · 2 alterações na fila" only offline with a queue (9.1g).
@Composable
internal fun OfflineQueueBanner(count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null)
            Text(
                text = pluralStringResource(R.plurals.category_map_offline_queue, count, count),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Why: "Seu mapa fica só neste aparelho…" while the account has no Google or Apple (9.1h).
@Composable
internal fun DeviceOnlyCard(onEvent: (CategoryMapUiEvent) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.category_map_device_only),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { onEvent(CategoryMapUiEvent.OnProfileClicked) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.category_map_open_profile))
            }
        }
    }
}

@Composable
internal fun MapIntroText(isOffline: Boolean) {
    Text(
        text = stringResource(if (isOffline) R.string.category_map_intro_offline else R.string.category_map_intro),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

// Why: "Seu mapa está vazio" (9.1e): with no term the category never comes from the title.
@Composable
internal fun EmptyMapContent(onEvent: (CategoryMapUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 40.dp,
                horizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.category_map_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.category_map_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = {
            onEvent(
                CategoryMapUiEvent.OnAddTermClicked(
                    categoryId = null,
                    text = "",
                ),
            )
        }) {
            Text(stringResource(R.string.category_map_add))
        }
        OutlinedButton(onClick = { onEvent(CategoryMapUiEvent.OnRestoreClicked) }) {
            Text(stringResource(R.string.category_map_restore))
        }
    }
}

@Composable
internal fun NoTermFoundContent(
    query: String,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.SearchOff, contentDescription = null)
        Text(
            text = stringResource(R.string.category_map_no_result, query),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.category_map_no_result_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = {
            onEvent(
                CategoryMapUiEvent.OnAddTermClicked(
                    categoryId = null,
                    text = query,
                ),
            )
        }) {
            Text(stringResource(R.string.category_map_add_query, query))
        }
    }
}

// Why: "2 termos com “uber”", each with what it matched marked and where it sends a title (9.1b).
@Composable
internal fun SearchResultsList(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val results = state.searchResults
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = pluralStringResource(R.plurals.category_map_results, results.size, results.size, state.query.trim()),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 8.dp,
            ),
        )
        results.forEachIndexed { index, result ->
            SearchResultRow(
                result = result,
                index = index,
                count = results.size,
                onEvent = onEvent,
            )
        }
        if (results.size > 1) {
            Text(
                text = stringResource(R.string.category_map_longest_wins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 8.dp,
                ),
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    result: MapSearchResult,
    index: Int,
    count: Int,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val term = result.term.term
    val marked = buildAnnotatedString {
        append(term.substring(0, result.matchStart))
        withStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
        ) { append(term.substring(result.matchStart, result.matchEnd)) }
        append(term.substring(result.matchEnd))
    }
    val path =
        result.parent?.let {
            stringResource(
                R.string.category_map_path,
                categoryName(it),
                categoryName(result.category),
            )
        }
            ?: categoryName(result.category)
    Surface(
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onEvent(CategoryMapUiEvent.OnTermClicked(result.term.key)) },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(category = result.parent ?: result.category)
            Column {
                Text(
                    text = marked,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Restaurar o mapa padrão?" (9.1f) says which of the person's own terms stay.
@Composable
internal fun RestoreMapDialog(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val own = state.ownTerms
    val ownText = when (own.size) {
        0 -> ""
        1 -> " " + stringResource(R.string.category_map_restore_own_one, own.first().term)
        else -> " " + pluralStringResource(R.plurals.category_map_restore_own_many, own.size, own.size)
    }
    AlertDialog(
        onDismissRequest = { onEvent(CategoryMapUiEvent.OnRestoreDismissed) },
        title = { Text(stringResource(R.string.category_map_restore_title)) },
        text = { Text(stringResource(R.string.category_map_restore_body) + ownText) },
        confirmButton = {
            TextButton(onClick = { onEvent(CategoryMapUiEvent.OnRestoreConfirmed) }) {
                Text(stringResource(R.string.category_map_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(CategoryMapUiEvent.OnRestoreDismissed) }) {
                Text(stringResource(R.string.category_map_cancel))
            }
        },
    )
}

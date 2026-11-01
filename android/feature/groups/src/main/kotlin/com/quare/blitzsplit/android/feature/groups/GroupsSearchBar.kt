package com.quare.blitzsplit.android.feature.groups

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope

/*
 * Why: while searching, the search bar takes the top app bar's place: back, what is typed and ✕, and under it "Tudo",
 * "Grupos", "Pessoas" and "Despesas". It takes the focus as it opens, so the keyboard comes up at once.
 */
@Composable
internal fun GroupsSearchBar(
    search: GroupsSearch,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Why: only for a new search; coming back from a result keeps the keyboard down over what was found.
    LaunchedEffect(Unit) { if (search.query.isEmpty()) focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .then(if (layout.isTablet) Modifier.widthIn(max = 560.dp) else Modifier)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onEvent(GroupsUiEvent.OnSearchClosed) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.groups_search_back),
                        )
                    }
                    SearchQueryField(
                        query = search.query,
                        onQueryChange = { onEvent(GroupsUiEvent.OnSearchQueryChanged(it)) },
                        onSearch = { focusManager.clearFocus() },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    )
                    if (search.query.isNotEmpty()) {
                        IconButton(onClick = { onEvent(GroupsUiEvent.OnSearchQueryChanged("")) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.groups_search_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            ConnectedButtonRow(
                labels = SearchScope.entries.map { stringResource(scopeLabelOf(it)) },
                selectedIndex = SearchScope.entries.indexOf(search.scope),
                groupLabel = stringResource(R.string.groups_search_scopes),
                onClick = { index -> onEvent(GroupsUiEvent.OnSearchScopeChanged(SearchScope.entries[index])) },
                height = 40.dp,
                showsCheck = false,
            )
        }
    }
}

@Composable
private fun SearchQueryField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.groups_search_placeholder)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                field()
            }
        },
    )
}

private fun scopeLabelOf(scope: SearchScope): Int = when (scope) {
    SearchScope.ALL -> R.string.groups_search_scope_all
    SearchScope.GROUPS -> R.string.groups_search_scope_groups
    SearchScope.PEOPLE -> R.string.groups_search_scope_people
    SearchScope.BILLS -> R.string.groups_search_scope_bills
}

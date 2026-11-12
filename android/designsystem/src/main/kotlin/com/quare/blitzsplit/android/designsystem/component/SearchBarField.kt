package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why: a search field over a list (the agenda's, "All expenses"'): the magnifier, what is typed, and ✕ once something
 * is, in a rounded bar; the keyboard's search key only closes the keyboard, the list already following each letter.
 */
@Composable
fun SearchBarField(
    query: String,
    placeholder: String,
    clearLabel: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .height(height)
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
                placeholder = placeholder,
                onQueryChange = onQueryChange,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = clearLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

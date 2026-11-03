package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/*
 * Why: Material's expressive ListItem rounds its corners and pads 10dp above and below; the app's rows sit in cards
 * and sheets that already shape them, with the 8dp the classic ListItem used, so the shape and padding stay as they
 * were.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlatListItem(
    containerColor: Color,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
) {
    ListItem(
        modifier = modifier,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        shapes = ListItemDefaults.shapes(shape = RectangleShape),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        content = headlineContent,
    )
}

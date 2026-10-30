package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val dialogWidth = 560.dp
private val wideDialogWidth = 920.dp

// Why: what a phone opens full screen is a card over the group on a tablet, wider while it lays out two columns.
@Composable
fun DialogCard(
    isWide: Boolean,
    spacing: Dp,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.padding(vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = if (isWide) wideDialogWidth else dialogWidth)
                .fillMaxWidth()
                .then(cardModifier),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )
        }
        SnackbarHost(snackbarHostState)
    }
}

package com.quare.blitzsplit.wear.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/*
 * Why: the connect screens (15.9 to 15.13): an icon, a title and a line or two, and one EdgeButton at the bottom.
 * A list, so the largest font still scrolls instead of cutting the text.
 */
@Composable
fun MessageScreen(
    title: String,
    body: String?,
    edgeButtonText: String,
    onEdgeButtonClick: () -> Unit,
    isEdgeButtonPrimary: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    extra: TransformingLazyColumnScope.() -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = listState,
        modifier = modifier,
        scrollIndicator = { ListScrollIndicator(listState) },
        edgeButton = {
            EdgeButton(
                onClick = onEdgeButtonClick,
                colors = if (isEdgeButtonPrimary) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
                modifier = Modifier.scrollable(
                    listState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                ),
            ) {
                Text(edgeButtonText)
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    icon()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    body?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            extra()
        }
    }
}

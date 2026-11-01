package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent

// Why: an invite no longer valid, or no internet to read it: what went wrong, then what to do, per size.
@Composable
internal fun ProblemContent(
    icon: ImageVector,
    title: String,
    message: String,
    displayLink: String?,
    onRetry: (() -> Unit)?,
    onScanAnother: (() -> Unit)?,
    layout: JoinLayout,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    val isColumn = layout == JoinLayout.COLUMN
    val alignment = if (isColumn) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (isColumn) TextAlign.Center else TextAlign.Start
    JoinColumnsLayout(
        layout = layout,
        start = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isColumn) 48.dp else 0.dp),
                horizontalAlignment = alignment,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = textAlign,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                )
                if (displayLink != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = displayLink,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp,
                            ),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        end = {},
        // Why: stacked beside what went wrong, side by side only in a tabletop's bottom half.
        actions = {
            ProblemActionsLayout(
                isRow = layout == JoinLayout.TABLETOP,
                onRetry = onRetry,
                onScanAnother = onScanAnother,
                onEvent = onEvent,
            )
        },
    )
}

// Why: on a tabletop the buttons share its bottom half's row; everywhere else they stack, the first one the main one.
@Composable
private fun ProblemActionsLayout(
    isRow: Boolean,
    onRetry: (() -> Unit)?,
    onScanAnother: (() -> Unit)?,
    onEvent: (JoinInviteUiEvent) -> Unit,
) {
    val buttons = @Composable { buttonModifier: Modifier ->
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = buttonModifier.heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.join_retry))
            }
        }
        OutlinedButton(
            onClick = { onEvent(JoinInviteUiEvent.OnGoHomeClicked) },
            modifier = buttonModifier.heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.join_go_home))
        }
        if (onScanAnother != null) {
            TextButton(
                onClick = onScanAnother,
                modifier = buttonModifier.heightIn(min = 56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = stringResource(R.string.join_scan_another),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
    if (isRow) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons(Modifier.weight(1f)) }
    } else {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { buttons(Modifier.fillMaxWidth()) }
    }
}

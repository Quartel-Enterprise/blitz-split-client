package com.quare.blitzsplit.android.feature.invite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiEvent
import com.quare.blitzsplit.feature.invite.presentation.model.InviteUiState

internal val qrMaxSize = 240.dp

// Why: what a phone lying down and a tabletop's top half have room for, the link beside or under it.
internal val compactQrSize = 196.dp

/*
 * Why: the QR code and the link with "Copiar". A tabletop sets them side by side in its top half, where a column
 * wouldn't fit; elsewhere the link goes under the code.
 */
@Composable
internal fun LinkCard(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isRow: Boolean = false,
    qrSize: Dp = qrMaxSize,
) {
    val link = state.displayLink
    val qrCode = state.qrCode
    if (link == null || qrCode == null) {
        PendingLinkBox(
            state = state,
            onEvent = onEvent,
            modifier = modifier,
        )
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        val description = stringResource(R.string.invite_qr_description)
        if (isRow) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrCodeImage(
                    qrCode = qrCode,
                    modifier = Modifier
                        .size(compactQrSize)
                        .semantics { contentDescription = description },
                )
                LinkRow(
                    link = link,
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QrCodeImage(
                    qrCode = qrCode,
                    modifier = Modifier
                        .widthIn(max = qrSize)
                        .fillMaxWidth()
                        .semantics { contentDescription = description },
                )
                LinkRow(
                    link = link,
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    link: String,
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = link,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FilledTonalButton(
            onClick = { onEvent(InviteUiEvent.OnCopyClicked) },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                imageVector = if (state.isLinkCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = stringResource(if (state.isLinkCopied) R.string.invite_copied else R.string.invite_copy),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
            )
        }
    }
}

// Why: black on white in both themes, as scanners read it best, with the quiet zone drawn around the modules.
@Composable
private fun QrCodeImage(
    qrCode: QrCode,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        // Why: the blank border a scanner needs around the code, in modules.
        val quietZone = 2
        val cells = qrCode.size + quietZone * 2
        val module = size.minDimension / cells
        for (row in 0 until qrCode.size) {
            for (column in 0 until qrCode.size) {
                if (!qrCode.isDark(row, column)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(
                        x = (column + quietZone) * module,
                        y = (row + quietZone) * module,
                    ),
                    // Why: a hair wider than the module, so no seam shows between two dark ones.
                    size = Size(module + 0.5f, module + 0.5f),
                )
            }
        }
    }
}

// Why: on its way while online, waiting for the internet while offline, or failed with a retry after a failed sync.
@Composable
private fun PendingLinkBox(
    state: InviteUiState,
    onEvent: (InviteUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val isCreating = state.isOnline && !state.isLinkFailed
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }.padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isCreating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        Text(
            text = stringResource(
                if (state.isLinkFailed) R.string.invite_failed_title else R.string.invite_pending_title,
            ),
            modifier = Modifier.padding(top = if (isCreating) 12.dp else 0.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                when {
                    state.isLinkFailed -> R.string.invite_failed_body
                    state.isOnline -> R.string.invite_pending_online
                    else -> R.string.invite_pending_offline
                },
            ),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.isLinkFailed) {
            TextButton(
                onClick = { onEvent(InviteUiEvent.OnRetryClicked) },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.invite_retry))
            }
        }
    }
}

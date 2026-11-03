package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private const val RECEIPT_LINE_COUNT = 4

// Why: "Lido do cupom" and "14 itens" under the description once a receipt was read (7.1e).
@Composable
internal fun ReceiptReadRow(state: SplitBillUiState) {
    val receipt = state.receipt?.takeIf { it.status == ReceiptStatus.READ } ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .padding(
                        start = 8.dp,
                        end = 12.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.split_bill_receipt_read),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (receipt.itemCount > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.split_bill_receipt_items,
                            receipt.itemCount,
                            receipt.itemCount,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/*
 * Why: "Não deu para ler o cupom…" with "Tirar outra foto" (7.1g), on top of the form like the conflict's banner; the
 * fields stay as they were, to fill in by hand.
 */
@Composable
internal fun ReceiptFailureBanner(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    if (!state.showsReceiptFailure) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 14.dp,
                end = 8.dp,
                bottom = 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.split_bill_receipt_failed),
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { onEvent(SplitBillUiEvent.OnScanReceiptClicked) },
                    enabled = state.canScanReceipt,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.split_bill_receipt_retake))
                }
            }
        }
    }
}

/*
 * Why: under "Divisão": "Ler cupom fiscal" until a receipt is picked (hidden on a tabletop, as the board draws it),
 * then "Foto do cupom", which the bill attaches once saved, with × to take it off.
 */
@Composable
internal fun ReceiptSection(
    state: SplitBillUiState,
    showsScanButton: Boolean,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val receipt = state.receipt
    when {
        receipt != null && receipt.hasPhoto -> ReceiptPhotoRow(
            isReading = receipt.status == ReceiptStatus.READING,
            onRemove = { onEvent(SplitBillUiEvent.OnReceiptRemoved) },
        )

        state.showsScanReceipt && showsScanButton -> ScanReceiptButton(state, onEvent)
    }
}

@Composable
private fun ScanReceiptButton(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
            onClick = { onEvent(SplitBillUiEvent.OnScanReceiptClicked) },
            enabled = state.canScanReceipt,
        ) {
            Icon(
                imageVector = Icons.Outlined.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.split_bill_scan_receipt),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = stringResource(
                if (state.isOnline) R.string.split_bill_scan_receipt_help else R.string.split_bill_scan_receipt_offline,
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReceiptPhotoRow(
    isReading: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(
                    start = 12.dp,
                    end = 8.dp,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReceiptThumbnailIcon()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = stringResource(R.string.split_bill_receipt_photo),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        if (isReading) {
                            R.string.split_bill_receipt_reading
                        } else {
                            R.string.split_bill_receipt_photo_visibility
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isReading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    strokeWidth = 3.dp,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.split_bill_receipt_remove),
                )
            }
        }
    }
}

// Why: the board's small receipt, a few printed lines on a slip.
@Composable
private fun ReceiptThumbnailIcon() {
    Column(
        modifier = Modifier
            .size(
                width = 40.dp,
                height = 48.dp,
            ).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
            .padding(
                horizontal = 7.dp,
                vertical = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(RECEIPT_LINE_COUNT) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index % 2 == 0) 1f else 0.6f)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

package com.quare.blitzsplit.android.feature.paymentkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiState

private val optionGap = 2.dp
private val optionInnerCorner = 8.dp
private val rowShape = RoundedCornerShape(20.dp)

// Why: each Pix type or Bitcoin network is one connected button, the picked one fully round.
@Composable
internal fun PaymentKeyTypeGrid(
    state: PaymentKeyUiState,
    columns: Int,
    isCompact: Boolean,
    onEvent: (PaymentKeyUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = state.types
    val isBitcoin = state.method == PaymentMethod.BITCOIN
    val height = when {
        !isBitcoin -> 48.dp
        isCompact -> 56.dp
        else -> 64.dp
    }
    val outerCorner = when {
        !isBitcoin -> 20.dp
        columns == 1 -> 28.dp
        else -> 32.dp
    }
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(optionGap),
    ) {
        types.chunked(columns).forEachIndexed { row, rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(optionGap),
            ) {
                rowTypes.forEachIndexed { column, type ->
                    val isSelected = type == state.type
                    KeyTypeOption(
                        type = type,
                        isSelected = isSelected,
                        shape = optionShapeOf(
                            row = row,
                            column = column,
                            rows = (types.size + columns - 1) / columns,
                            columns = columns,
                            isSelected = isSelected,
                            outerCorner = outerCorner,
                        ),
                        onClick = { onEvent(PaymentKeyUiEvent.OnTypeSelected(type)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(height),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyTypeOption(
    type: PaymentKeyType,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val networkIcon = type.networkIcon
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).padding(horizontal = if (networkIcon == null) 4.dp else 16.dp),
            contentAlignment = if (networkIcon == null) Alignment.Center else Alignment.CenterStart,
        ) {
            if (networkIcon == null) {
                Text(
                    text = stringResource(type.optionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = networkIcon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(type.optionRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        type.networkDetailRes?.let { detail ->
                            Text(
                                text = stringResource(detail),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun optionShapeOf(
    row: Int,
    column: Int,
    rows: Int,
    columns: Int,
    isSelected: Boolean,
    outerCorner: Dp,
): Shape {
    if (isSelected) return RoundedCornerShape(percent = 50)
    val isTop = row == 0
    val isBottom = row == rows - 1
    val isStart = column == 0
    val isEnd = column == columns - 1
    return RoundedCornerShape(
        topStart = if (isTop && isStart) outerCorner else optionInnerCorner,
        topEnd = if (isTop && isEnd) outerCorner else optionInnerCorner,
        bottomEnd = if (isBottom && isEnd) outerCorner else optionInnerCorner,
        bottomStart = if (isBottom && isStart) outerCorner else optionInnerCorner,
    )
}

@Composable
internal fun AcceptedFormatsCard(
    type: PaymentKeyType,
    modifier: Modifier = Modifier,
) {
    val samples = if (type == PaymentKeyType.LIGHTNING) {
        listOf(
            R.string.payment_key_accepted_lightning_address_sample to R.string.payment_key_accepted_lightning_address,
            R.string.payment_key_accepted_lnurl_sample to R.string.payment_key_accepted_lnurl,
        )
    } else {
        listOf(
            R.string.payment_key_accepted_segwit_sample to R.string.payment_key_accepted_segwit,
            R.string.payment_key_accepted_taproot_sample to R.string.payment_key_accepted_taproot,
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.payment_key_accepted_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            samples.forEach { (sample, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(sample),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = stringResource(name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CpfNoticeCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.payment_key_cpf_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun DefaultKeySwitchRow(
    state: PaymentKeyUiState,
    onEvent: (PaymentKeyUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPix = state.method == PaymentMethod.PIX
    val detail = stringResource(
        if (isPix) R.string.payment_key_default_pix_detail else R.string.payment_key_default_bitcoin_detail,
    )
    val currentDefault = state.currentDefaultType
    val suffix = when {
        state.isDefaultLocked && state.isEditing -> stringResource(R.string.payment_key_default_locked_editing)

        state.isDefaultLocked -> stringResource(R.string.payment_key_default_locked_new)

        currentDefault != null -> stringResource(
            R.string.payment_key_default_now,
            stringResource(currentDefault.defaultTypeRes),
        )

        else -> null
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = state.isDefault,
                    enabled = !state.isDefaultLocked,
                    role = Role.Switch,
                    onValueChange = { onEvent(PaymentKeyUiEvent.OnDefaultChanged(it)) },
                ).padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isPix) {
                            R.string.payment_key_default_pix_title
                        } else {
                            R.string.payment_key_default_bitcoin_title
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(detail, suffix).joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.isDefault,
                onCheckedChange = null,
                enabled = !state.isDefaultLocked,
                thumbContent = if (state.isDefault) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
internal fun DeleteKeyButton(
    onEvent: (PaymentKeyUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = { onEvent(PaymentKeyUiEvent.OnDeleteClicked) },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.payment_key_delete),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
internal fun PaymentKeyFooter(
    method: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (method == PaymentMethod.PIX) Icons.Outlined.Shield else Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                if (method ==
                    PaymentMethod.PIX
                ) {
                    R.string.payment_key_footer_pix
                } else {
                    R.string.payment_key_footer_bitcoin
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val PaymentKeyType.networkIcon: ImageVector?
    get() = when (this) {
        PaymentKeyType.LIGHTNING -> Icons.Outlined.Bolt
        PaymentKeyType.ONCHAIN -> Icons.Outlined.Link
        PaymentKeyType.CPF, PaymentKeyType.PHONE, PaymentKeyType.EMAIL, PaymentKeyType.RANDOM -> null
    }

private val PaymentKeyType.networkDetailRes: Int?
    get() = when (this) {
        PaymentKeyType.LIGHTNING -> R.string.payment_key_network_lightning_detail
        PaymentKeyType.ONCHAIN -> R.string.payment_key_network_onchain_detail
        PaymentKeyType.CPF, PaymentKeyType.PHONE, PaymentKeyType.EMAIL, PaymentKeyType.RANDOM -> null
    }

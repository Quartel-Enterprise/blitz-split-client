package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pix
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.profile.presentation.model.PaymentKeyItem
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

private val segmentOuterCorner = 20.dp
private val segmentInnerCorner = 6.dp
private val keyIconShape = RoundedCornerShape(12.dp)

// Why: one connected row per key, Pix first and each method's default first, then the row that adds one.
@Composable
internal fun PaymentKeysSection(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val count = state.paymentKeys.size + 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_section_payment_keys),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.profile_payment_keys_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.paymentKeys.forEachIndexed { index, key ->
                PaymentKeyItemRow(
                    key = key,
                    shape = segmentShapeOf(
                        index = index,
                        count = count,
                    ),
                    onClick = { onEvent(ProfileUiEvent.OnPaymentKeyClicked(key.id)) },
                )
            }
            AddPaymentKeyRow(
                shape = segmentShapeOf(
                    index = count - 1,
                    count = count,
                ),
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun PaymentKeyItemRow(
    key: PaymentKeyItem,
    shape: Shape,
    onClick: () -> Unit,
) {
    val isPix = key.type.method == PaymentMethod.PIX
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(key.type.rowTitleRes),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    text = key.format.toRowValue(key.maskedValue),
                    fontFamily = FontFamily.Monospace,
                )
            },
            leadingContent = {
                KeyMethodIcon(
                    icon = key.type.rowIcon,
                    containerColor = if (isPix) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = if (isPix) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
            },
            trailingContent = if (key.isDefault) {
                { DefaultKeyBadge() }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        )
    }
}

@Composable
private fun KeyMethodIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = containerColor,
                shape = keyIconShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = contentColor,
        )
    }
}

@Composable
private fun DefaultKeyBadge() {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = stringResource(R.string.profile_payment_key_default),
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 3.dp,
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// Why: a key is either Pix or Bitcoin, so the native menu asks which before the key screen opens.
@Composable
private fun AddPaymentKeyRow(
    shape: Shape,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { isMenuOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DashedAddIcon()
                Text(
                    text = stringResource(R.string.profile_payment_keys_add),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_payment_keys_add_pix)) },
                onClick = {
                    isMenuOpen = false
                    onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.PIX))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Pix,
                        contentDescription = null,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_payment_keys_add_bitcoin)) },
                onClick = {
                    isMenuOpen = false
                    onEvent(ProfileUiEvent.OnAddPaymentKeyClicked(PaymentMethod.BITCOIN))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyBitcoin,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun DashedAddIcon() {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(40.dp)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun segmentShapeOf(
    index: Int,
    count: Int,
): Shape {
    val top: Dp = if (index == 0) segmentOuterCorner else segmentInnerCorner
    val bottom: Dp = if (index == count - 1) segmentOuterCorner else segmentInnerCorner
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomEnd = bottom,
        bottomStart = bottom,
    )
}

@Composable
private fun PaymentKeyFormat.toRowValue(maskedValue: String): String = when (this) {
    PaymentKeyFormat.SEGWIT -> stringResource(R.string.profile_payment_key_value_segwit, maskedValue)

    PaymentKeyFormat.TAPROOT -> stringResource(R.string.profile_payment_key_value_taproot, maskedValue)

    PaymentKeyFormat.LNURL -> stringResource(R.string.profile_payment_key_value_lnurl, maskedValue)

    PaymentKeyFormat.CPF,
    PaymentKeyFormat.PHONE,
    PaymentKeyFormat.EMAIL,
    PaymentKeyFormat.RANDOM,
    PaymentKeyFormat.LIGHTNING_ADDRESS,
    -> maskedValue
}

@get:StringRes
private val PaymentKeyType.rowTitleRes: Int
    get() = when (this) {
        PaymentKeyType.CPF -> R.string.profile_payment_key_cpf
        PaymentKeyType.PHONE -> R.string.profile_payment_key_phone
        PaymentKeyType.EMAIL -> R.string.profile_payment_key_email
        PaymentKeyType.RANDOM -> R.string.profile_payment_key_random
        PaymentKeyType.LIGHTNING -> R.string.profile_payment_key_lightning
        PaymentKeyType.ONCHAIN -> R.string.profile_payment_key_onchain
    }

private val PaymentKeyType.rowIcon: ImageVector
    get() = when (this) {
        PaymentKeyType.CPF, PaymentKeyType.PHONE, PaymentKeyType.EMAIL, PaymentKeyType.RANDOM -> Icons.Outlined.Pix
        PaymentKeyType.LIGHTNING -> Icons.Outlined.Bolt
        PaymentKeyType.ONCHAIN -> Icons.Outlined.Link
    }

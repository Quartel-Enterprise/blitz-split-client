package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pix
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType

private val tileShape = RoundedCornerShape(12.dp)
private const val ICON_RATIO = 0.55f

/*
 * Why: a payment method's tile, the same on Profile, its form, Split a bill and Bill: a card filled with its own
 * brand-neutral swatch, Pix and Bitcoin in the keys' colors, the rest on the neutral tile. Never a bank's or a card
 * brand's logo.
 */
@Composable
fun PayingMethodTile(
    type: PayingMethodType,
    cardColor: CardColor?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val (container, content) = when {
        type.isCard -> (cardColor ?: CardColor.GRAPHITE).swatch to Color.White

        type == PayingMethodType.PIX ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer

        type == PayingMethodType.BITCOIN ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer

        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = container,
                shape = tileShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            modifier = Modifier.size(size * ICON_RATIO),
            tint = content,
        )
    }
}

// Why: what the person called it, or the type's own name for a method with no name.
@Composable
fun PayingMethod.label(): String = name.ifBlank { stringResource(type.nameRes) }

@get:StringRes
val PayingMethodType.nameRes: Int
    get() = when (this) {
        PayingMethodType.CREDIT_CARD -> R.string.designsystem_paying_method_credit_card
        PayingMethodType.DEBIT_CARD -> R.string.designsystem_paying_method_debit_card
        PayingMethodType.CASH -> R.string.designsystem_paying_method_cash
        PayingMethodType.PIX -> R.string.designsystem_paying_method_pix
        PayingMethodType.BANK_TRANSFER -> R.string.designsystem_paying_method_bank_transfer
        PayingMethodType.BITCOIN -> R.string.designsystem_paying_method_bitcoin
        PayingMethodType.OTHER -> R.string.designsystem_paying_method_other
    }

@get:StringRes
val CardColor.nameRes: Int
    get() = when (this) {
        CardColor.PURPLE -> R.string.designsystem_card_color_purple
        CardColor.BLUE -> R.string.designsystem_card_color_blue
        CardColor.GREEN -> R.string.designsystem_card_color_green
        CardColor.ORANGE -> R.string.designsystem_card_color_orange
        CardColor.RED -> R.string.designsystem_card_color_red
        CardColor.PINK -> R.string.designsystem_card_color_pink
        CardColor.GOLD -> R.string.designsystem_card_color_gold
        CardColor.GRAPHITE -> R.string.designsystem_card_color_graphite
    }

// Why: the same swatches in light and dark, each dark enough for the white card glyph (3:1 or more).
val CardColor.swatch: Color
    get() = when (this) {
        CardColor.PURPLE -> Color(0xFF6D4AB8)
        CardColor.BLUE -> Color(0xFF1565C0)
        CardColor.GREEN -> Color(0xFF2E7D32)
        CardColor.ORANGE -> Color(0xFFE65100)
        CardColor.RED -> Color(0xFFC62828)
        CardColor.PINK -> Color(0xFFAD1457)
        CardColor.GOLD -> Color(0xFF9A7500)
        CardColor.GRAPHITE -> Color(0xFF455A64)
    }

private val PayingMethodType.icon: ImageVector
    get() = when (this) {
        PayingMethodType.CREDIT_CARD, PayingMethodType.DEBIT_CARD -> Icons.Outlined.CreditCard
        PayingMethodType.CASH -> Icons.Outlined.Payments
        PayingMethodType.PIX -> Icons.Outlined.Pix
        PayingMethodType.BANK_TRANSFER -> Icons.Outlined.AccountBalance
        PayingMethodType.BITCOIN -> Icons.Outlined.CurrencyBitcoin
        PayingMethodType.OTHER -> Icons.Outlined.MoreHoriz
    }

/*
 * Why: "Paid with" on Split a bill: the method picked (or None), whose menu lists None and each of the person's
 * methods. "Only you see this." under it, since the group never sees it.
 */
@Composable
fun PaidWithPickerRow(
    methods: List<PayingMethod>,
    picked: PayingMethod?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Surface(
                onClick = { isMenuOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                PaidWithLine(
                    method = picked,
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.designsystem_paid_with_none)) },
                    onClick = {
                        isMenuOpen = false
                        onPick(null)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.RemoveCircleOutline,
                            contentDescription = null,
                        )
                    },
                )
                methods.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.label()) },
                        onClick = {
                            isMenuOpen = false
                            onPick(method.id)
                        },
                        leadingIcon = {
                            PayingMethodTile(
                                type = method.type,
                                cardColor = method.cardColor,
                                size = 28.dp,
                            )
                        },
                    )
                }
            }
        }
        PaidWithPrivateLabel(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// Why: Bill's "Paid with", the person's own mark, read only.
@Composable
fun PaidWithCard(
    method: PayingMethod,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            PaidWithLine(
                method = method,
                trailing = {},
            )
        }
        PaidWithPrivateLabel(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun PaidWithLine(
    method: PayingMethod?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 56.dp)
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (method != null) {
            PayingMethodTile(
                type = method.type,
                cardColor = method.cardColor,
                size = 32.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.designsystem_paid_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = method?.label() ?: stringResource(R.string.designsystem_paid_with_none),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun PaidWithPrivateLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.designsystem_paid_with_private),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: a card's color on its form, one round swatch each, named for screen readers.
@Composable
fun CardColorPicker(
    colors: List<CardColor>,
    selected: CardColor,
    onSelected: (CardColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colors.forEach { color ->
            ColorSwatchButton(
                label = stringResource(color.nameRes),
                isSelected = color == selected,
                container = color.swatch,
                content = Color.White,
                onClick = { onSelected(color) },
            )
        }
    }
}

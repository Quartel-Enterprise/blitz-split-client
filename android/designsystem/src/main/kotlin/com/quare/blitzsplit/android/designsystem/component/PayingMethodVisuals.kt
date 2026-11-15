package com.quare.blitzsplit.android.designsystem.component

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pix
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.theme.rgbOf
import com.quare.blitzsplit.core.expenses.domain.model.CardTints
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType

// Why: the corner grows with the tile (12dp at 40dp), so a chip's 18dp tile stays a rounded square, not a dot.
private const val CORNER_RATIO = 0.3f
private const val ICON_RATIO = 0.55f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/*
 * Why: a payment method's tile, the same on Profile, its form, Split a bill and Bill: a card filled with its color, a
 * category's at its light data tone in both themes (CardTints.fillRgbOf), Pix and Bitcoin in the keys' colors, the
 * rest on the neutral tile. Never a bank's or a card brand's logo.
 */
@Composable
fun PayingMethodTile(
    type: PayingMethodType,
    cardColor: CategoryTint?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val (container, content) = when {
        type.isCard -> rgbOf(CardTints.fillRgbOf(cardColor)) to Color.White

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
                shape = RoundedCornerShape(size * CORNER_RATIO),
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

private val PayingMethodType.icon: ImageVector
    get() = when (this) {
        PayingMethodType.CREDIT_CARD -> Icons.Outlined.CreditCard

        // Why: a wallet, so the two cards tell apart in a list and a menu at a glance (Pierre's call).
        PayingMethodType.DEBIT_CARD -> Icons.Outlined.AccountBalanceWallet

        PayingMethodType.CASH -> Icons.Outlined.Payments

        PayingMethodType.PIX -> Icons.Outlined.Pix

        PayingMethodType.BANK_TRANSFER -> Icons.Outlined.AccountBalance

        PayingMethodType.BITCOIN -> Icons.Outlined.CurrencyBitcoin

        PayingMethodType.OTHER -> Icons.Outlined.MoreHoriz
    }

// Why: the glyphs of Paid with's chips, live and locked alike, at a chip's icon size.
@Composable
private fun ChipIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(AssistChipDefaults.IconSize),
    )
}

/*
 * Why: "Paid with" while this device's user doesn't pay the bill (Pierre's call, beta feedback of November 15, 2026):
 * drawn as a disabled chip on None, yet still taking the tap, which raises a snackbar saying why it can't change.
 */
@Composable
fun LockedPaidWithChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.designsystem_paid_with_none)
    val description = listOf(
        stringResource(R.string.designsystem_paid_with),
        none,
        stringResource(R.string.designsystem_paid_with_only_payers),
    ).joinToString(separator = ". ")
    val dimmed = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = none,
                maxLines = 1,
            )
        },
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick {
                onClick()
                true
            }
        },
        leadingIcon = { ChipIcon(Icons.Outlined.CreditCard) },
        trailingIcon = { ChipIcon(Icons.Outlined.ExpandMore) },
        // Why: the disabled chip's look, while the chip itself stays enabled so the tap gets through.
        colors = AssistChipDefaults.assistChipColors(
            labelColor = dimmed,
            leadingIconContentColor = dimmed,
            trailingIconContentColor = dimmed,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = false),
    )
}

/*
 * Why: "Paid with" on Split a bill, as discreet as a chip (Pierre's call, feedback of November 15, 2026): the method
 * picked (or None) with its tile, whose menu lists None and each of the person's methods. Screen readers hear "Paid
 * with", the method and "Only you see this.", since the group never sees it.
 */
@Composable
fun PaidWithChip(
    methods: List<PayingMethod>,
    picked: PayingMethod?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val pickedLabel = picked?.label() ?: stringResource(R.string.designsystem_paid_with_none)
    val description = listOf(
        stringResource(R.string.designsystem_paid_with),
        pickedLabel,
        stringResource(R.string.designsystem_paid_with_private),
    ).joinToString(separator = ". ")
    Box(modifier = modifier) {
        AssistChip(
            onClick = { isMenuOpen = true },
            label = {
                Text(
                    text = pickedLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.DropdownList
                    if (enabled) {
                        onClick {
                            isMenuOpen = true
                            true
                        }
                    } else {
                        disabled()
                    }
                },
            enabled = enabled,
            leadingIcon = {
                if (picked != null) {
                    PayingMethodTile(
                        type = picked.type,
                        cardColor = picked.cardColor,
                        size = AssistChipDefaults.IconSize,
                    )
                } else {
                    ChipIcon(Icons.Outlined.CreditCard)
                }
            },
            trailingIcon = { ChipIcon(Icons.Outlined.ExpandMore) },
        )
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
}

// Why: "Add a payment method" in Paid with's place while the person has none; the method saved comes back picked.
@Composable
fun AddPaidWithButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Text(
            text = stringResource(R.string.designsystem_paid_with_add),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
        )
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
            PaidWithLine(method = method)
        }
        PaidWithPrivateLabel(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun PaidWithLine(method: PayingMethod) {
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
        PayingMethodTile(
            type = method.type,
            cardColor = method.cardColor,
            size = 32.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.designsystem_paid_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = method.label(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

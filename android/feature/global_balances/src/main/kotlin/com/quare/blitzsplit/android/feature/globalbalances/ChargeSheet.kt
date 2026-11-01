package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.InfoNoteBanner
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesGroupDialog
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val chargeWidth = 560.dp

/*
 * Why: "Cobrar" moves no money and sends nothing by itself: a message to edit, Enviar for the system's share sheet and
 * Copiar mensagem. A bottom sheet on a phone (560dp wide lying down), a 560dp dialog on a tablet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChargeSheet(
    charge: ChargeDraft,
    pixKey: PaymentKey?,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    val onDismiss = { onEvent(GlobalBalancesUiEvent.OnChargeDismissed) }
    if (rememberUsesGroupDialog()) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.widthIn(max = chargeWidth),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ChargeColumn(
                    charge = charge,
                    pixKey = pixKey,
                    onEvent = onEvent,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetMaxWidth = chargeWidth,
        ) {
            ChargeColumn(
                charge = charge,
                pixKey = pixKey,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            )
        }
    }
}

@Composable
private fun ChargeColumn(
    charge: ChargeDraft,
    pixKey: PaymentKey?,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = charge.name ?: stringResource(DesignR.string.designsystem_former_member)
    val message = chargeMessageOf(charge, name, pixKey)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChargeHeader(
            charge = charge,
            name = name,
            onClose = { onEvent(GlobalBalancesUiEvent.OnChargeDismissed) },
        )
        if (charge.isGuest) {
            InfoNoteBanner(text = stringResource(R.string.global_balances_charge_guest, name))
        }
        OutlinedTextField(
            value = message,
            onValueChange = { onEvent(GlobalBalancesUiEvent.OnChargeMessageChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.global_balances_charge_message)) },
            minLines = 3,
        )
        if (pixKey != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.global_balances_charge_pix),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.global_balances_charge_pix_key,
                            keyTypeOf(pixKey.type),
                            pixKey.value,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = charge.includesPixKey,
                    onCheckedChange = { onEvent(GlobalBalancesUiEvent.OnChargePixKeyToggled(it)) },
                    enabled = charge.canTogglePixKey,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onEvent(GlobalBalancesUiEvent.OnChargeCopyClicked(message)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.global_balances_charge_copy))
            }
            Button(
                onClick = { onEvent(GlobalBalancesUiEvent.OnChargeShareClicked(message)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.global_balances_charge_send),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ChargeHeader(
    charge: ChargeDraft,
    name: String,
    onClose: () -> Unit,
) {
    val money = rememberMoneyFormatter(charge.currency)
    val groups = if (charge.groupNames.size > 1) {
        pluralStringResource(R.plurals.global_balances_groups, charge.groupNames.size, charge.groupNames.size)
    } else {
        charge.groupNames.firstOrNull().orEmpty()
    }
    // Why: where the amount goes in "Te deve %1$s · %2$s", so it can be drawn in the primary color.
    val amountSlot = "\u0000"
    val owes = stringResource(R.string.global_balances_charge_owes, amountSlot, groups)
    val amount = money.toText(charge.amountMinor)
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = charge.initials,
            color = charge.color ?: AvatarColor.default,
            size = 48.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.global_balances_charge_title, name),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildAnnotatedString {
                    append(owes.substringBefore(amountSlot))
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(amount)
                    }
                    append(owes.substringAfter(amountSlot))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.global_balances_charge_close))
        }
    }
}

/*
 * Why: the message in this language, from the debt, the groups and, if wanted, the Pix key; a guest's also says what
 * went into the bill. Once edited, the text typed.
 */
@Composable
private fun chargeMessageOf(
    charge: ChargeDraft,
    name: String,
    pixKey: PaymentKey?,
): String {
    val money = rememberMoneyFormatter(charge.currency)
    val amount = money.toText(charge.amountMinor)
    val groupNames = nameListOf(charge.groupNames)
    val from = stringResource(
        if (charge.groupNames.size >
            1
        ) {
            R.string.global_balances_message_groups
        } else {
            R.string.global_balances_message_group
        },
        groupNames,
    )
    val bills = nameListOf(charge.guestBills)
    val paid = charge.guestPaidMinor
    val body = when {
        !charge.isGuest || charge.guestBills.isEmpty() -> stringResource(
            R.string.global_balances_message,
            name,
            amount,
            from,
        )

        paid != null -> stringResource(
            R.string.global_balances_message_guest_paid,
            name,
            amount,
            from,
            bills,
            money.toText(paid),
        )

        else -> stringResource(R.string.global_balances_message_guest, name, amount, from, bills)
    }
    val composed = if (charge.includesPixKey && pixKey != null) {
        stringResource(R.string.global_balances_message_pix, body, pixKey.value)
    } else {
        body
    }
    return charge.editedMessage ?: composed
}

@Composable
private fun keyTypeOf(type: PaymentKeyType): String = stringResource(
    when (type) {
        PaymentKeyType.CPF -> R.string.global_balances_key_cpf
        PaymentKeyType.PHONE -> R.string.global_balances_key_phone
        PaymentKeyType.RANDOM -> R.string.global_balances_key_random
        else -> R.string.global_balances_key_email
    },
)

package com.quare.blitzsplit.android.feature.registerpayment

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.component.QrCodeImage
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.platform.copyText
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState

private val pixQrSize = 112.dp

// Why: how the sheet or dialog around "Pagar Ana" (4.5) lays it out.
internal enum class PayWithKeyLayout {
    // Why: a phone's sheet, a tablet's 560dp dialog and a book's right half.
    OneColumn,

    // Why: the 640dp sheet of a phone lying down: who, how and the default key left; the rest and the buttons right.
    TwoColumns,
}

/*
 * Why: "Pagar Ana" (4.5), what Registrar pagamento shows while this device's user pays someone with a key: the key
 * to copy, the Pix code with the amount in it, and "Marcar como pago" once paid in the bank or the wallet. The app
 * moves no money. Android itself confirms what was copied.
 */
@Composable
internal fun PayWithKeyContent(
    state: RegisterPaymentUiState,
    layout: PayWithKeyLayout,
    money: MoneyFormatter,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onCopy: (String) -> Unit = { text -> context.copyText(text) }
    val who: @Composable ColumnScope.() -> Unit = {
        PayHeader(
            state = state,
            money = money,
        )
        if (state.showsKeyMethodPicker) {
            KeyMethodPicker(
                state = state,
                onEvent = onEvent,
            )
        }
        state.defaultKey?.let { key ->
            DefaultKeyCard(
                key = key,
                receiverName = nameOf(state.to),
                onCopy = onCopy,
            )
        }
    }
    val rest: @Composable ColumnScope.() -> Unit = {
        state.pixCode?.let { code ->
            PixCodeCard(
                state = state,
                code = code,
                money = money,
                onCopy = onCopy,
            )
        }
        if (state.otherKeys.isNotEmpty()) {
            OtherKeysList(
                keys = state.otherKeys,
                receiverName = nameOf(state.to),
                onCopy = onCopy,
            )
        }
        PayNoteSection(state)
    }
    val spacing = Arrangement.spacedBy(16.dp)
    when (layout) {
        PayWithKeyLayout.OneColumn -> Column(
            modifier = modifier,
            verticalArrangement = spacing,
        ) {
            who()
            rest()
            PayButtonRow(
                state = state,
                onEvent = onEvent,
            )
        }

        PayWithKeyLayout.TwoColumns -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = spacing,
                content = who,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = spacing,
            ) {
                rest()
                PayButtonRow(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: "Pagar Ana", where the debt comes from ("Apê 302 · você deve") and how much, in the color of what you owe.
@Composable
private fun PayHeader(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonFaceBadge(state.to)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.register_payment_pay_title, nameOf(state.to)),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.groupName?.let { stringResource(R.string.register_payment_pay_group, it) }
                    ?: pluralStringResource(
                        R.plurals.register_payment_pay_groups,
                        state.groupParts.size,
                        state.groupParts.size,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = money.toText(state.debtMinor),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// Why: "Forma de pagamento", Pix or Bitcoin, only while the receiver has keys of both.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KeyMethodPicker(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        state.keyMethods.forEachIndexed { index, method ->
            ToggleButton(
                checked = method == state.shownKeyMethod,
                onCheckedChange = { onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(method)) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Tab },
                shapes = if (index == 0) {
                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                } else {
                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                },
            ) {
                Icon(
                    imageVector = if (method == PaymentMethod.PIX) Icons.Outlined.Diamond else Icons.Outlined.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                )
                Text(
                    text = stringResource(methodLabelOf(method)),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

// Why: "Chave padrão de Ana", whole, with its type and "Copiar chave".
@Composable
private fun DefaultKeyCard(
    key: ReceiverKey,
    receiverName: String,
    onCopy: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.register_payment_pay_default_key, receiverName),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(keyTypeLabelOf(key.type)),
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 2.dp,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                text = key.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = { onCopy(key.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.register_payment_pay_copy_key),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

// Why: "Pix copia e cola", the default key with the whole debt in it, and its QR code for paying from another device.
@Composable
private fun PixCodeCard(
    state: RegisterPaymentUiState,
    code: String,
    money: MoneyFormatter,
    onCopy: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.pixQrCode?.let { qrCode ->
                QrCodeImage(
                    qrCode = qrCode,
                    modifier = Modifier.width(pixQrSize),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.register_payment_pay_pix_code),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.register_payment_pay_pix_code_amount, money.toText(state.debtMinor)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = { onCopy(code) },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(stringResource(R.string.register_payment_pay_copy_code)) }
            }
        }
    }
}

// Why: "Outras chaves de Ana", each with its own copy button.
@Composable
private fun OtherKeysList(
    keys: List<ReceiverKey>,
    receiverName: String,
    onCopy: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.register_payment_pay_other_keys, receiverName),
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 6.dp,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        keys.forEach { key ->
            val type = stringResource(keyTypeLabelOf(key.type))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .padding(
                            start = 16.dp,
                            end = 4.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = key.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { onCopy(key.value) }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(
                                R.string.register_payment_pay_copy_other,
                                type,
                                receiverName,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// Why: the app moves no money, so it says where to pay and to check the name the bank shows; offline, that it waits.
@Composable
private fun PayNoteSection(state: RegisterPaymentUiState) {
    Text(
        text = if (state.shownKeyMethod == PaymentMethod.PIX) {
            stringResource(R.string.register_payment_pay_note_pix, nameOf(state.to))
        } else {
            stringResource(R.string.register_payment_pay_note_bitcoin)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.showsOfflineNotice) {
        OfflineNoticeBanner(
            title = stringResource(R.string.register_payment_offline_title),
            message = stringResource(R.string.register_payment_offline_message),
            isCompact = true,
        )
    }
}

// Why: "Depois" closes; "Marcar como pago" records the whole debt.
@Composable
private fun PayButtonRow(
    state: RegisterPaymentUiState,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { onEvent(RegisterPaymentUiEvent.OnCloseClicked) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            enabled = !state.isSaving,
        ) { Text(stringResource(R.string.register_payment_pay_later)) }
        FilledTonalButton(
            onClick = { onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked) },
            modifier = Modifier
                .weight(1.6f)
                .heightIn(min = 56.dp),
            enabled = state.canMarkAsPaid,
        ) { Text(stringResource(R.string.register_payment_pay_mark_paid)) }
    }
}

@StringRes
private fun methodLabelOf(method: PaymentMethod): Int = when (method) {
    PaymentMethod.PIX -> R.string.register_payment_pay_method_pix
    PaymentMethod.BITCOIN -> R.string.register_payment_pay_method_bitcoin
}

@StringRes
private fun keyTypeLabelOf(type: PaymentKeyType): Int = when (type) {
    PaymentKeyType.CPF -> R.string.register_payment_pay_type_cpf
    PaymentKeyType.PHONE -> R.string.register_payment_pay_type_phone
    PaymentKeyType.EMAIL -> R.string.register_payment_pay_type_email
    PaymentKeyType.RANDOM -> R.string.register_payment_pay_type_random
    PaymentKeyType.LIGHTNING -> R.string.register_payment_pay_type_lightning
    PaymentKeyType.ONCHAIN -> R.string.register_payment_pay_type_onchain
}

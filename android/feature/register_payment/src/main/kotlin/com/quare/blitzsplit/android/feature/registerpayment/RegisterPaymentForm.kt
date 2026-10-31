package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AmountTextField
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.relativeDateTimeTextOf
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val faceSize = 48.dp

// Why: who paid whom and how much they owe, then the amount, how and when it was paid, and what recording it does.
@Composable
internal fun RegisterPaymentFormColumn(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onWhenClick: () -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PaymentHeader(
            state = state,
            money = money,
        )
        AmountSection(
            state = state,
            money = money,
            onEvent = onEvent,
        )
        MethodSection(
            method = state.method,
            onEvent = onEvent,
        )
        Surface(
            onClick = onWhenClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            ListItem(
                overlineContent = { Text(stringResource(DesignR.string.designsystem_when)) },
                headlineContent = {
                    Text(
                        relativeDateTimeTextOf(
                            epochMillis = state.paidAt,
                            day = state.paidDay,
                            dates = dates,
                        ),
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Event, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            )
        }
        InfoLine(state)
        if (state.showsOfflineNotice) {
            OfflineNoticeBanner(
                title = stringResource(R.string.register_payment_offline_title),
                message = stringResource(R.string.register_payment_offline_message),
                isCompact = true,
            )
        }
    }
}

@Composable
private fun PaymentHeader(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonFaceBadge(state.from)
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PersonFaceBadge(state.to)
        }
        Text(
            text = titleOf(state),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = debtTextOf(state, state.debtMinor, money),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PersonFaceBadge(person: PaymentPerson?) {
    // Why: a blank face for someone no longer in the group, as Saldos draws them.
    if (person == null) {
        Box(
            modifier = Modifier
                .size(faceSize)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
        return
    }
    AvatarBadge(
        initial = person.initials,
        color = person.avatarColor,
        size = faceSize,
    )
}

@Composable
private fun AmountSection(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AmountTextField(
            value = state.amountText,
            onValueChange = { onEvent(RegisterPaymentUiEvent.OnAmountChanged(it)) },
            label = stringResource(R.string.register_payment_amount),
            symbol = money.symbol,
            help = amountHelpOf(state, money),
            isError = state.showsOverDebtError,
            imeAction = ImeAction.Done,
        )
        FilterChip(
            selected = state.isFullDebt,
            onClick = { onEvent(RegisterPaymentUiEvent.OnFullDebtClicked) },
            label = { Text(stringResource(R.string.register_payment_full_debt)) },
            leadingIcon = if (state.isFullDebt) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MethodSection(
    method: SettlementMethod,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.register_payment_method),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettlementMethod.entries.forEach { option ->
                FilterChip(
                    selected = option == method,
                    onClick = { onEvent(RegisterPaymentUiEvent.OnMethodPicked(option)) },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }
    }
}

// Why: what recording does, and for whom when this device's user is neither of the two.
@Composable
private fun InfoLine(state: RegisterPaymentUiState) {
    val forOthers = if (state.kind == PaymentKind.BETWEEN_OTHERS) {
        stringResource(R.string.register_payment_for_others, nameOf(state.from), nameOf(state.to))
    } else {
        null
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = listOfNotNull(stringResource(R.string.register_payment_info), forOthers).joinToString(" "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

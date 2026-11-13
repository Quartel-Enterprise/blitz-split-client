package com.quare.blitzsplit.android.feature.registerpayment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AmountTextField
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.FlatListItem
import com.quare.blitzsplit.android.designsystem.component.OfflineNoticeBanner
import com.quare.blitzsplit.android.designsystem.component.PersonGroupsCard
import com.quare.blitzsplit.android.designsystem.component.opensProfile
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.relativeDateTimeTextOf
import com.quare.blitzsplit.android.designsystem.format.wordGapUi
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PayCopyTarget
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
    onCopy: (String, PayCopyTarget) -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RegisterPaymentSummaryColumn(
            state = state,
            money = money,
            onEvent = onEvent,
        )
        RegisterPaymentDetailsColumn(
            state = state,
            dates = dates,
            onWhenClick = onWhenClick,
            onCopy = onCopy,
            onEvent = onEvent,
        )
    }
}

// Why: what to read, the canvas' first column lying down and the top half on a tabletop: who paid whom and how much.
@Composable
internal fun RegisterPaymentSummaryColumn(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PaymentHeader(
            state = state,
            money = money,
            onEvent = onEvent,
        )
        AmountSection(
            state = state,
            money = money,
            onEvent = onEvent,
        )
    }
}

// Why: the controls, the canvas' second column lying down and the bottom half on a tabletop: how, when, what it does.
@Composable
internal fun RegisterPaymentDetailsColumn(
    state: RegisterPaymentUiState,
    dates: DateTimeTextFormatter,
    onWhenClick: () -> Unit,
    onCopy: (String, PayCopyTarget) -> Unit,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MethodSection(
            method = state.method,
            methods = state.methods,
            onEvent = onEvent,
        )
        FormPixKeysSection(
            state = state,
            onCopy = onCopy,
        )
        Surface(
            onClick = onWhenClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            FlatListItem(
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
        }
        SimplifiedNoteLine(state)
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
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonFaceBadge(
                person = state.from,
                onClick = state.from?.let { { onEvent(RegisterPaymentUiEvent.OnPersonClicked(it.id)) } },
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PersonFaceBadge(
                person = state.to,
                onClick = state.to?.let { { onEvent(RegisterPaymentUiEvent.OnPersonClicked(it.id)) } },
            )
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
internal fun PersonFaceBadge(
    person: PaymentPerson?,
    size: Dp = faceSize,
    // Why: Registrar pagamento's faces open the person's profile (beta feedback 18); "Quem pagou a quem?"'s are its rows'.
    onClick: (() -> Unit)? = null,
) {
    // Why: a blank face for someone no longer in the group, as Saldos draws them.
    if (person == null) {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
        return
    }
    // Why: TalkBack says "Você" for this device's user, as the sentence under the faces does.
    val name = if (person.isMe) stringResource(DesignR.string.designsystem_you) else person.name
    AvatarBadge(
        initial = person.initials,
        color = person.avatarColor,
        photoUrl = person.photoUrl,
        size = size,
        modifier = Modifier
            .clip(CircleShape)
            .semantics { if (onClick != null) contentDescription = name }
            .opensProfile(onClick),
    )
}

@Composable
private fun AmountSection(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    // Why: across several groups only the whole net settles, so the groups it closes show instead of the field.
    if (state.isAmountLocked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonGroupsCard(
                groups = state.groupParts,
                netMinor = state.groupParts.sumOf { it.amountMinor },
                money = money,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            Text(
                text = stringResource(R.string.register_payment_one_per_group),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AmountTextField(
            value = state.amountText,
            onValueChange = { onEvent(RegisterPaymentUiEvent.OnAmountChanged(it)) },
            label = stringResource(R.string.register_payment_amount),
            symbol = money.symbol,
            help = amountHelpOf(state, money),
            isError = state.showsOverDebtError,
            imeAction = ImeAction.Done,
        )
        FullDebtButton(
            isFullDebt = state.isFullDebt,
            onClick = { onEvent(RegisterPaymentUiEvent.OnFullDebtClicked) },
        )
    }
}

/*
 * Why: a plain action, not a chip, so it doesn't compete with "Como foi pago"'s chips (beta feedback 5). It is
 * done, with its check, while the amount already is the whole debt.
 */
@Composable
private fun FullDebtButton(
    isFullDebt: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = !isFullDebt,
        // Why: done, not unavailable, so its check reads in the primary color, as on the web.
        colors = ButtonDefaults.textButtonColors(
            disabledContentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
    ) {
        if (isFullDebt) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(stringResource(R.string.register_payment_full_debt))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MethodSection(
    method: SettlementMethod,
    methods: List<SettlementMethod>,
    onEvent: (RegisterPaymentUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.register_payment_method),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEach { option ->
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
            text = listOfNotNull(stringResource(R.string.register_payment_info), forOthers).joinToString(wordGapUi()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

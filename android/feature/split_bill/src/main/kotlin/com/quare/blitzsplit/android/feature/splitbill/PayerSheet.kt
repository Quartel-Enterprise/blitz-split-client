package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmountRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private const val SUBTITLE_SEPARATOR = " · "

// Why: "Quem pagou" marks a person until "Pronto"; dismissing it keeps whoever paid before.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayerSheet(
    state: SplitBillUiState,
    selection: PayerSelection,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(SplitBillUiEvent.OnPayerDismissed) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        PayerColumn(
            state = state,
            selection = selection,
            money = money,
            onEvent = onEvent,
        )
    }
}

// Why: "Várias pessoas pagaram" takes the place of "Quem pagou"; dismissing it keeps whoever paid before.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeveralPayersSheet(
    amounts: PayerAmounts,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(SplitBillUiEvent.OnSeveralPayersDismissed) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        SeveralPayersColumn(
            amounts = amounts,
            money = money,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PayerColumn(
    state: SplitBillUiState,
    selection: PayerSelection,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val amount = state.amountMinor?.takeIf { state.isAmountValid }?.let(money::toText)
    val subtitle = listOfNotNull(amount, state.description.trim().takeIf { it.isNotEmpty() })
        .joinToString(SUBTITLE_SEPARATOR)
    val rowCount = state.people.size + 1
    SheetColumn(
        title = stringResource(R.string.split_bill_payer),
        subtitle = subtitle,
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            state.people.forEachIndexed { index, person ->
                SheetOptionRow(
                    title = personNameOf(person),
                    subtitle = stringResource(R.string.split_bill_not_joined).takeIf { !person.hasJoined },
                    isSelected = person.id == selection.memberId,
                    shape = groupedRowShapeOf(index, rowCount),
                    onClick = { onEvent(SplitBillUiEvent.OnPayerPicked(person.id)) },
                ) {
                    AvatarBadge(
                        initial = person.initials,
                        color = person.avatarColor,
                        size = 36.dp,
                    )
                }
            }
            // Why: marked while the bill has several payers and nobody else was picked; it opens their amounts.
            SheetOptionRow(
                title = stringResource(R.string.split_bill_several_payers),
                subtitle = stringResource(R.string.split_bill_several_payers_hint),
                isSelected = selection.memberId == null,
                shape = groupedRowShapeOf(rowCount - 1, rowCount),
                onClick = { onEvent(SplitBillUiEvent.OnSeveralPayersClicked) },
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Button(
            onClick = { onEvent(SplitBillUiEvent.OnPayerDone) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.split_bill_done)) }
    }
}

// Why: one field per person, and whether what they paid adds up: "R$ 312,00 de R$ 312,00. Fechou."
@Composable
private fun SeveralPayersColumn(
    amounts: PayerAmounts,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val subtitle = if (amounts.totalMinor > 0) {
        stringResource(R.string.split_bill_several_payers_lead, money.toText(amounts.totalMinor))
    } else {
        stringResource(R.string.split_bill_several_payers_lead_no_total)
    }
    SheetColumn(
        title = stringResource(R.string.split_bill_several_payers_title),
        subtitle = subtitle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            amounts.rows.forEachIndexed { index, row ->
                PayerAmountRowItem(
                    row = row,
                    money = money,
                    shape = groupedRowShapeOf(index, amounts.rows.size),
                    onEvent = onEvent,
                )
            }
        }
        payersStatusOrNull(amounts, money)?.let { status ->
            Text(
                text = status,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (amounts.addsUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { onEvent(SplitBillUiEvent.OnSeveralPayersDone) },
            enabled = amounts.addsUp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.split_bill_done)) }
    }
}

// Why: "Fechou" once the amounts add up, what is missing or over while there is a total to compare with.
@Composable
private fun payersStatusOrNull(
    amounts: PayerAmounts,
    money: MoneyFormatter,
): String? = when {
    amounts.addsUp -> stringResource(
        R.string.split_bill_payers_add_up,
        money.toText(amounts.paidMinor),
        money.toText(amounts.totalMinor),
    )

    amounts.showsMismatch -> payersMismatchOf(amounts, money)

    else -> null
}

@Composable
internal fun SheetColumn(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun PayerAmountRowItem(
    row: PayerAmountRow,
    money: MoneyFormatter,
    shape: Shape,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val person = row.person
    val name = person?.let { personNameOf(it) } ?: stringResource(R.string.split_bill_former_member)
    SheetRowSurface(
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (person == null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            )
        } else {
            AvatarBadge(
                initial = person.initials,
                color = person.avatarColor,
                size = 36.dp,
            )
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CompactNumberField(
            value = row.amountText,
            onValueChange = { onEvent(SplitBillUiEvent.OnPayerAmountChanged(row.memberId, it)) },
            // Why: "Quanto Pierre pagou" reads better to TalkBack than "Quanto Você pagou".
            label = stringResource(R.string.split_bill_payer_amount, person?.name ?: name),
            placeholder = money.zeroPlaceholder,
            prefix = money.symbol,
            suffix = null,
            keyboardType = KeyboardType.Decimal,
            isError = false,
            width = amountFieldWidth,
        )
    }
}

@Composable
internal fun SheetOptionRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    SheetRowSurface(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
    }
}

// Why: a row of the payer sheets, read with the others as one card.
@Composable
private fun SheetRowSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(
                    start = 16.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@DevicePreviews
@Composable
private fun PayerColumnPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            val state = createPreviewBillState()
            PayerColumn(
                state = state,
                selection = PayerSelection(memberId = state.payerId),
                money = rememberMoneyFormatter(state.currency),
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SeveralPayersColumnPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            SeveralPayersColumn(
                amounts = createPreviewPayerAmounts(),
                money = rememberMoneyFormatter(GroupCurrency.BRL),
                onEvent = {},
            )
        }
    }
}

package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private const val SUBTITLE_SEPARATOR = " · "
private val outerRadius = 20.dp
private val innerRadius = 6.dp

// Why: "Quem pagou" marks a person until "Pronto"; dismissing it keeps whoever paid before.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayerSheet(
    state: SplitBillUiState,
    selectedId: String,
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
            selectedId = selectedId,
            money = money,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PayerColumn(
    state: SplitBillUiState,
    selectedId: String,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val amount = state.amountMinor?.takeIf { state.isAmountValid }?.let(money::toText)
    val subtitle = listOfNotNull(amount, state.description.trim().takeIf { it.isNotEmpty() })
        .joinToString(SUBTITLE_SEPARATOR)
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
                text = stringResource(R.string.split_bill_payer),
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
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            state.people.forEachIndexed { index, person ->
                PayerOptionRow(
                    person = person,
                    isSelected = person.id == selectedId,
                    shape = RoundedCornerShape(
                        topStart = if (index == 0) outerRadius else innerRadius,
                        topEnd = if (index == 0) outerRadius else innerRadius,
                        bottomStart = if (index == state.people.lastIndex) outerRadius else innerRadius,
                        bottomEnd = if (index == state.people.lastIndex) outerRadius else innerRadius,
                    ),
                    onClick = { onEvent(SplitBillUiEvent.OnPayerPicked(person.id)) },
                )
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

@Composable
private fun PayerOptionRow(
    person: BillPerson,
    isSelected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
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
        ) {
            AvatarBadge(
                initial = person.initials,
                color = person.avatarColor,
                size = 36.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = personNameOf(person),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!person.hasJoined) {
                    Text(
                        text = stringResource(R.string.split_bill_not_joined),
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
}

@DevicePreviews
@Composable
private fun PayerColumnPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            val state = createPreviewBillState()
            PayerColumn(
                state = state,
                selectedId = state.payerId,
                money = rememberMoneyFormatter(state.currency),
                onEvent = {},
            )
        }
    }
}

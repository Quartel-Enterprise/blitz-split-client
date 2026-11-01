package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitPerson
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitRow
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiState
import java.util.Locale

private const val PERCENT_POINTS = 100.0

@Composable
internal fun PeopleSection(
    state: DefaultSplitUiState,
    money: MoneyFormatter,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    SectionLabel(stringResource(state.mode.listLabelRes))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.rows.forEachIndexed { index, row ->
            PersonRow(
                row = row,
                state = state,
                shape = createGroupedShape(index, state.rows.size),
                money = money,
                onEvent = onEvent,
            )
        }
    }
    if (state.mode == DefaultSplitMode.INCOME) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.default_split_income_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PersonRow(
    row: DefaultSplitRow,
    state: DefaultSplitUiState,
    shape: Shape,
    money: MoneyFormatter,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    val person = row.person
    val name = personNameOf(person)
    val toggle = if (state.mode == DefaultSplitMode.EQUAL) {
        Modifier.toggleable(
            value = person.isIncluded,
            role = Role.Checkbox,
            onValueChange = { onEvent(DefaultSplitUiEvent.OnPersonToggled(person.id)) },
        )
    } else {
        Modifier
    }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = toggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(
                    start = 16.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.mode == DefaultSplitMode.EQUAL) {
                Checkbox(
                    checked = person.isIncluded,
                    onCheckedChange = null,
                )
            }
            AvatarBadge(
                initial = person.initials,
                color = person.avatarColor,
                size = 36.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = personSubtitleOf(row, state, money),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.showsPercentError && row.weight == 0L && state.usesPercent) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            PersonValueComponent(
                person = person,
                name = name,
                state = state,
                money = money,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun PersonValueComponent(
    person: DefaultSplitPerson,
    name: String,
    state: DefaultSplitUiState,
    money: MoneyFormatter,
    onEvent: (DefaultSplitUiEvent) -> Unit,
) {
    when (state.mode) {
        DefaultSplitMode.EQUAL -> Unit

        DefaultSplitMode.SHARES -> PartsStepperRow(
            parts = person.parts,
            label = stringResource(R.string.default_split_parts_of, name),
            onDecrease = { onEvent(DefaultSplitUiEvent.OnPartsDecreased(person.id)) },
            onIncrease = { onEvent(DefaultSplitUiEvent.OnPartsIncreased(person.id)) },
        )

        DefaultSplitMode.PERCENT -> NumberField(
            value = person.percentText,
            description = stringResource(R.string.default_split_percent_of, name),
            isError = state.showsPercentError,
            prefix = null,
            suffix = "%",
            isWide = false,
            onChange = { onEvent(DefaultSplitUiEvent.OnPercentChanged(person.id, it)) },
        )

        DefaultSplitMode.INCOME -> NumberField(
            value = person.incomeText,
            description = stringResource(R.string.default_split_income_of, name),
            isError = false,
            prefix = money.symbol,
            suffix = null,
            isWide = true,
            onChange = { onEvent(DefaultSplitUiEvent.OnIncomeChanged(person.id, it)) },
        )
    }
}

@Composable
internal fun PartsStepperRow(
    parts: Long,
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = false) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onDecrease,
            enabled = parts > 0,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.default_split_less),
            )
        }
        Text(
            text = parts.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = onIncrease,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.default_split_more),
            )
        }
    }
}

@Composable
internal fun NumberField(
    value: String,
    description: String,
    isError: Boolean,
    prefix: String?,
    suffix: String?,
    isWide: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .width(if (isWide) 148.dp else 96.dp)
            .semantics { contentDescription = description },
        singleLine = true,
        isError = isError,
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isWide) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}

// Why: "1 de 6 · 16,7%", "75% de cada conta", "R$ 75,00 de cada R$ 100,00", "Renda por mês · 75% de cada conta".
@Composable
private fun personSubtitleOf(
    row: DefaultSplitRow,
    state: DefaultSplitUiState,
    money: MoneyFormatter,
): String {
    val percent = row.basisPoints / PERCENT_POINTS
    val subtitle = when (state.mode) {
        DefaultSplitMode.EQUAL -> if (row.weight > 0) {
            stringResource(
                R.string.default_split_row_equal,
                state.includedCount,
                String.format(Locale.getDefault(), "%.1f", percent),
            )
        } else {
            stringResource(R.string.default_split_row_out)
        }

        DefaultSplitMode.SHARES -> stringResource(R.string.default_split_row_shares, kotlin.math.round(percent).toInt())

        DefaultSplitMode.PERCENT -> stringResource(
            R.string.default_split_row_percent,
            money.toText(row.exampleMinor ?: 0L),
            money.toText(state.exampleTotalMinor),
        )

        DefaultSplitMode.INCOME -> stringResource(R.string.default_split_row_income, row.weight)
    }
    return if (row.person.isNew) stringResource(R.string.default_split_row_new, subtitle) else subtitle
}

@Composable
internal fun personNameOf(person: DefaultSplitPerson): String =
    if (person.isMe) stringResource(R.string.default_split_me, person.name) else person.name

private val DefaultSplitMode.listLabelRes: Int
    get() = when (this) {
        DefaultSplitMode.EQUAL -> R.string.default_split_list_equal
        DefaultSplitMode.SHARES -> R.string.default_split_list_shares
        DefaultSplitMode.PERCENT -> R.string.default_split_list_percent
        DefaultSplitMode.INCOME -> R.string.default_split_list_income
    }

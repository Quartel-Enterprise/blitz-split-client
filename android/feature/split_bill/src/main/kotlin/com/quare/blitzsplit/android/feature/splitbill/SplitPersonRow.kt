package com.quare.blitzsplit.android.feature.splitbill

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow

private const val DIMMED_ALPHA = 0.5f
private const val PERCENT_SUFFIX = "%"
internal val amountFieldWidth = 128.dp
private val percentFieldWidth = 88.dp

// Why: one person in Dividir; who is out of the split stays listed, dimmed, so they can be put back.
@Composable
internal fun SplitPersonRow(
    row: SplitRow,
    mode: SplitMode,
    isIn: Boolean,
    // Why: "Pagou a conta", or "Pagou parte da conta" for one of several payers; null for whoever didn't pay.
    @StringRes paidLabel: Int?,
    isWithoutValue: Boolean,
    isNobodyIn: Boolean,
    shape: Shape,
    minHeight: Dp,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val isDimmed = !isIn && mode != SplitMode.AMOUNT
    val name = personNameOf(row.person)
    val toggleModifier = if (mode == SplitMode.EQUAL) {
        Modifier.toggleable(
            value = row.isIncluded,
            role = Role.Checkbox,
            onValueChange = { onEvent(SplitBillUiEvent.OnPersonToggled(row.person.id)) },
        )
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(toggleModifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(
                    start = if (mode == SplitMode.EQUAL) 4.dp else 16.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode == SplitMode.EQUAL) {
                Checkbox(
                    checked = row.isIncluded,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 8.dp),
                    colors = if (isNobodyIn) {
                        CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.error)
                    } else {
                        CheckboxDefaults.colors()
                    },
                )
            }
            AvatarBadge(
                initial = row.person.initials,
                color = row.person.avatarColor,
                size = 36.dp,
                modifier = Modifier.alpha(if (isDimmed) DIMMED_ALPHA else 1f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDimmed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rowSubtitleOf(
                        row = row,
                        mode = mode,
                        isIn = isIn,
                        paidLabel = paidLabel,
                        isWithoutValue = isWithoutValue,
                        money = money,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWithoutValue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            when (mode) {
                SplitMode.EQUAL -> Text(
                    text = row.shareMinor?.let(money::toText) ?: stringResource(R.string.split_bill_no_share),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                SplitMode.AMOUNT -> CompactNumberField(
                    value = row.valueText,
                    onValueChange = { onEvent(SplitBillUiEvent.OnPersonValueChanged(row.person.id, it)) },
                    label = stringResource(R.string.split_bill_person_amount, name),
                    placeholder = money.zeroPlaceholder,
                    prefix = money.symbol,
                    suffix = null,
                    keyboardType = KeyboardType.Decimal,
                    isError = isWithoutValue,
                    width = amountFieldWidth,
                )

                SplitMode.PERCENT -> CompactNumberField(
                    value = row.valueText,
                    onValueChange = { onEvent(SplitBillUiEvent.OnPersonValueChanged(row.person.id, it)) },
                    label = stringResource(R.string.split_bill_person_percent, name),
                    placeholder = "0",
                    prefix = null,
                    suffix = PERCENT_SUFFIX,
                    keyboardType = KeyboardType.Number,
                    isError = false,
                    width = percentFieldWidth,
                )

                SplitMode.SHARES -> PartsStepperRow(
                    row = row,
                    name = name,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun rowSubtitleOf(
    row: SplitRow,
    mode: SplitMode,
    isIn: Boolean,
    @StringRes paidLabel: Int?,
    isWithoutValue: Boolean,
    money: MoneyFormatter,
): String = when (mode) {
    SplitMode.EQUAL -> stringResource(
        when {
            !row.isIncluded -> R.string.split_bill_row_out
            paidLabel != null -> paidLabel
            else -> R.string.split_bill_row_in
        },
    )

    SplitMode.AMOUNT -> stringResource(
        when {
            isWithoutValue -> R.string.split_bill_row_no_value
            paidLabel != null -> paidLabel
            !row.person.hasJoined -> R.string.split_bill_not_joined
            else -> R.string.split_bill_row_value
        },
    )

    SplitMode.PERCENT, SplitMode.SHARES -> row.shareMinor?.let(money::toText)
        ?: stringResource(if (isIn) R.string.split_bill_no_share else R.string.split_bill_row_out)
}

// Why: the canvas's 44dp field, shorter than a text field's 56dp, so six people fit a phone without scrolling.
@Composable
internal fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    prefix: String?,
    suffix: String?,
    keyboardType: KeyboardType,
    isError: Boolean,
    width: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(width)
            .semantics { contentDescription = label },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .border(
                        border = BorderStroke(if (isError || isFocused) 2.dp else 1.dp, borderColor),
                        shape = RoundedCornerShape(8.dp),
                    ).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                prefix?.let { AffixText(it) }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    innerTextField()
                }
                suffix?.let { AffixText(it) }
            }
        },
    )
}

@Composable
private fun AffixText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PartsStepperRow(
    row: SplitRow,
    name: String,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val label = stringResource(R.string.split_bill_person_parts, name)
    Row(
        modifier = Modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { onEvent(SplitBillUiEvent.OnPartsDecreased(row.person.id)) },
            modifier = Modifier.size(36.dp),
            enabled = row.parts > 0,
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.split_bill_parts_decrease),
            )
        }
        Text(
            text = row.parts.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onEvent(SplitBillUiEvent.OnPartsIncreased(row.person.id)) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.split_bill_parts_increase),
            )
        }
    }
}

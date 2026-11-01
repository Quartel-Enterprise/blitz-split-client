package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AmountTextField
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: what to read first: the group the bill goes to, how much it was and what it was.
@Composable
internal fun BillFieldsSection(
    state: SplitBillUiState,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    GroupChip(
        state = state,
        onEvent = onEvent,
    )
    AmountField(
        state = state,
        money = money,
        onEvent = onEvent,
    )
    OutlinedTextField(
        value = state.description,
        onValueChange = { onEvent(SplitBillUiEvent.OnDescriptionChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.split_bill_description)) },
        placeholder = { Text(stringResource(R.string.split_bill_description_placeholder)) },
        supportingText = if (state.showsDescriptionError) {
            { Text(stringResource(R.string.split_bill_description_error)) }
        } else {
            null
        },
        isError = state.showsDescriptionError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
    )
}

// Why: the group of the bill; a new one opens "Rachar em qual grupo?" from it, once there is another group.
@Composable
private fun GroupChip(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val changeLabel = stringResource(R.string.split_bill_group_change, state.groupName)
    Surface(
        onClick = { onEvent(SplitBillUiEvent.OnGroupClicked) },
        enabled = state.canChangeGroup,
        modifier = Modifier.semantics {
            if (state.canChangeGroup) contentDescription = changeLabel
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Why: a group that can't change reads as plain, not as a disabled control.
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(
                    start = 4.dp,
                    end = if (state.canChangeGroup) 10.dp else 14.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(
                initials = state.groupInitials,
                color = state.groupColor,
                size = 32.dp,
            )
            Text(
                text = state.groupName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.canChangeGroup) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/*
 * Why: a new bill opens on the amount, its keyboard up, as the canvas draws it focused; not when back from Dividir, and
 * only once "Rachar em qual grupo?" no longer covers it.
 */
@Composable
private fun AmountField(
    state: SplitBillUiState,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isAmountEmpty = state.amountText.isEmpty()
    val isAskingGroup = state.groupSelection != null
    LaunchedEffect(focusRequester, isAskingGroup) {
        if (isAmountEmpty && !isAskingGroup) focusRequester.requestFocus()
    }
    val help = listOfNotNull(
        amountHelpOrNull(
            state = state,
            money = money,
        ),
        // Why: editing a bill split by amount, a new total rescales everyone's part.
        stringResource(R.string.split_bill_amount_proportional).takeIf { state.showsProportionalAmountHint },
    ).joinToString("\n").ifEmpty { null }
    AmountTextField(
        value = state.amountText,
        onValueChange = { onEvent(SplitBillUiEvent.OnAmountChanged(it)) },
        label = stringResource(R.string.split_bill_amount),
        symbol = money.symbol,
        help = help,
        isError = state.showsAmountError,
        modifier = Modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun amountHelpOrNull(
    state: SplitBillUiState,
    money: MoneyFormatter,
): String? {
    val refund = state.refundMinor
    val myShare = state.myShareMinor
    return when {
        state.showsAmountError -> stringResource(R.string.split_bill_amount_error)
        refund != null -> stringResource(R.string.split_bill_refund, money.toText(refund))
        myShare != null -> stringResource(R.string.split_bill_my_share, money.toText(myShare))
        else -> null
    }
}

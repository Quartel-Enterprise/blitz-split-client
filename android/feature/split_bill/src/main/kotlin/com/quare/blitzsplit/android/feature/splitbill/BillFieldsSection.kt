package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    GroupChip(state)
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

// Why: the group of the bill, which this screen can't change yet.
@Composable
private fun GroupChip(state: SplitBillUiState) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(
                    start = 4.dp,
                    end = 14.dp,
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
        }
    }
}

// Why: a new bill opens on the amount, its keyboard up, as the canvas draws it focused; not when back from Dividir.
@Composable
private fun AmountField(
    state: SplitBillUiState,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isAmountEmpty = state.amountText.isEmpty()
    LaunchedEffect(focusRequester) {
        if (isAmountEmpty) focusRequester.requestFocus()
    }
    val help = amountHelpOrNull(
        state = state,
        money = money,
    )
    OutlinedTextField(
        value = state.amountText,
        onValueChange = { onEvent(SplitBillUiEvent.OnAmountChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        label = { Text(stringResource(R.string.split_bill_amount)) },
        prefix = {
            Text(
                text = money.symbol,
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingText = help?.let { text ->
            {
                Text(
                    text = text,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        isError = state.showsAmountError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
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

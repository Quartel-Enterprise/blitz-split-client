package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AmountTextField
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.CurrencyMenuContent
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.freeTextKeyboardOptions
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: what to read first: the group the bill goes to, what it was and how much it was (beta feedback 10).
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
    DescriptionField(
        state = state,
        onEvent = onEvent,
    )
    if (!state.isEditing && state.categoryMatches.size > 1) {
        MatchedTermsRow(state)
    }
    ReceiptReadRow(
        state = state,
        onEvent = onEvent,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AmountField(
            state = state,
            money = money,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
        CurrencyChip(
            state = state,
            onEvent = onEvent,
        )
    }
}

// Why: the bill's currency beside the total, the group's until another is picked; the list is Novo grupo's "Moeda".
@Composable
private fun CurrencyChip(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.split_bill_currency_change, state.currency.code)
    Box(modifier = Modifier.padding(top = 8.dp)) {
        Surface(
            onClick = { isMenuOpen = true },
            modifier = Modifier
                .height(56.dp)
                .semantics { contentDescription = label },
            enabled = state.canChangeCurrency,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.currency.code,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(
            expanded = isMenuOpen && state.canChangeCurrency,
            onDismissRequest = { isMenuOpen = false },
        ) {
            CurrencyMenuContent(
                currencies = state.currencies,
                selected = state.currency,
                onSelected = { currency ->
                    isMenuOpen = false
                    onEvent(SplitBillUiEvent.OnCurrencyPicked(currency))
                },
            )
        }
    }
}

/*
 * Why: a new bill opens on the description, its keyboard up, as the canvas draws it focused; not when back from
 * Dividir, and only once "Rachar em qual grupo?" no longer covers it.
 */
@Composable
private fun DescriptionField(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isNewBill = !state.isEditing && state.description.isEmpty() && state.amountText.isEmpty()
    val isAskingGroup = state.groupSelection != null
    LaunchedEffect(focusRequester, isAskingGroup) {
        if (isNewBill && !isAskingGroup && !state.isFormLocked) focusRequester.requestFocus()
    }
    val categoryLabel = stringResource(R.string.split_bill_category_change, billCategoryPathOf(state))
    OutlinedTextField(
        value = state.description,
        onValueChange = { onEvent(SplitBillUiEvent.OnDescriptionChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        enabled = !state.isFormLocked,
        label = { Text(stringResource(R.string.split_bill_description)) },
        placeholder = { Text(stringResource(R.string.split_bill_description_placeholder)) },
        // Why: the category's icon opens "Categoria" (7.7).
        leadingIcon = {
            IconButton(
                onClick = { onEvent(SplitBillUiEvent.OnCategoryClicked) },
                modifier = Modifier.semantics { contentDescription = categoryLabel },
                enabled = !state.isFormLocked,
            ) {
                CategoryBadge(
                    category = state.category,
                    size = 32.dp,
                )
            }
        },
        supportingText = {
            Text(
                text = if (state.showsDescriptionError) {
                    stringResource(R.string.split_bill_description_error)
                } else {
                    categoryHintOf(state)
                },
            )
        },
        isError = state.showsDescriptionError,
        singleLine = true,
        keyboardOptions = freeTextKeyboardOptions.copy(imeAction = ImeAction.Next),
    )
}

// Why: every word of the map the description has, the one that chose marked (7.1l).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchedTermsRow(state: SplitBillUiState) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.categoryMatches.forEachIndexed { index, match ->
            val isWinner = index == 0
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isWinner) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isWinner) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (isWinner) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = match.term,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 4.dp,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/*
 * Why: the group of the bill; a new one opens "Rachar em qual grupo?" from it, once there is another group. A group
 * that can't change is a plain label (no button role for TalkBack), not a disabled control.
 */
@Composable
private fun GroupChip(
    state: SplitBillUiState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val shape = CircleShape
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface
    if (state.canChangeGroup) {
        val changeLabel = stringResource(R.string.split_bill_group_change, state.groupName)
        Surface(
            onClick = { onEvent(SplitBillUiEvent.OnGroupClicked) },
            modifier = Modifier.semantics { contentDescription = changeLabel },
            shape = shape,
            color = color,
            contentColor = contentColor,
        ) {
            GroupChipContent(state)
        }
    } else {
        Surface(
            shape = shape,
            color = color,
            contentColor = contentColor,
        ) {
            GroupChipContent(state)
        }
    }
}

@Composable
private fun GroupChipContent(state: SplitBillUiState) {
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
            photoUrl = state.groupPhotoUrl,
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

@Composable
private fun AmountField(
    state: SplitBillUiState,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        imeAction = ImeAction.Done,
        enabled = !state.isFormLocked,
        modifier = modifier,
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

package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.paidBySeveralOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

private val outerRadius = 20.dp
private val innerRadius = 6.dp

// Why: "Dividir", drawn over Rachar conta while the split is being edited; it changes the bill only on "Pronto".
@Composable
internal fun SplitEditorContent(
    state: SplitBillUiState,
    editor: SplitEditor,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(state.currency)
    val rowMinHeight = if (rememberDeviceLayout().isLandscape) 52.dp else 60.dp
    SplitBillFormLayout(
        chrome = FormChrome(
            title = stringResource(R.string.split_bill_split_title),
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.split_bill_back),
            onNavigate = { onEvent(SplitBillUiEvent.OnSplitDismissed) },
            confirmLabel = stringResource(R.string.split_bill_done),
            isConfirmEnabled = editor.addsUp,
            onConfirm = { onEvent(SplitBillUiEvent.OnSplitDone) },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = false,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            SplitSummaryRow(
                state = state,
                editor = editor,
                money = money,
            )
            ConnectedButtonRow(
                labels = SplitMode.entries.map { stringResource(it.labelRes) },
                selectedIndex = SplitMode.entries.indexOf(editor.mode),
                groupLabel = stringResource(R.string.split_bill_modes),
                onClick = { index -> onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.entries[index])) },
                itemRole = Role.RadioButton,
                height = 44.dp,
                innerRadius = innerRadius,
                showsCheck = false,
            )
            SplitStatusCard(
                editor = editor,
                money = money,
            )
            Text(
                text = when (editor.mode) {
                    SplitMode.EQUAL -> stringResource(R.string.split_bill_hint_equal)
                    SplitMode.AMOUNT -> stringResource(R.string.split_bill_hint_amount, money.toText(0L))
                    SplitMode.PERCENT -> stringResource(R.string.split_bill_hint_percent)
                    SplitMode.SHARES -> stringResource(R.string.split_bill_hint_shares)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        second = {
            SplitPeopleList(
                editor = editor,
                hasSeveralPayers = state.severalPayers != null,
                rowMinHeight = rowMinHeight,
                money = money,
                onEvent = onEvent,
            )
        },
    )
}

private val SplitMode.labelRes: Int
    get() = when (this) {
        SplitMode.EQUAL -> R.string.split_bill_mode_equal
        SplitMode.AMOUNT -> R.string.split_bill_mode_amount
        SplitMode.PERCENT -> R.string.split_bill_mode_percent
        SplitMode.SHARES -> R.string.split_bill_mode_shares
    }

// Why: "Mercado · você pagou" over the total being split.
@Composable
private fun SplitSummaryRow(
    state: SplitBillUiState,
    editor: SplitEditor,
    money: MoneyFormatter,
) {
    val several = state.severalPayers
    val payer = state.payer
    val description = state.description.trim()
    val paid = when {
        several != null -> paidBySeveralOf(payerNamesOf(several))
        payer == null -> null
        payer.isMe && description.isEmpty() -> stringResource(R.string.split_bill_summary_you_paid_alone)
        payer.isMe -> stringResource(R.string.split_bill_summary_you_paid)
        else -> stringResource(R.string.split_bill_summary_paid, payer.name)
    }
    val caption = listOfNotNull(description.takeIf { it.isNotEmpty() }, paid)
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (caption.isNotEmpty()) {
                Text(
                    text = if (caption.size == 2) {
                        stringResource(R.string.split_bill_summary, caption[0], caption[1])
                    } else {
                        caption.first()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = money.toText(editor.totalMinor),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SplitPeopleList(
    editor: SplitEditor,
    hasSeveralPayers: Boolean,
    rowMinHeight: Dp,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val includedIds = editor.included.map { it.id }.toSet()
    val withoutValueIds = if (editor.addsUp) emptySet() else editor.withoutValue.map { it.id }.toSet()
    val isNobodyIn = editor.mode == SplitMode.EQUAL && editor.includedCount == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                when (editor.mode) {
                    SplitMode.EQUAL -> R.string.split_bill_list_equal
                    SplitMode.AMOUNT -> R.string.split_bill_list_amount
                    SplitMode.PERCENT -> R.string.split_bill_list_percent
                    SplitMode.SHARES -> R.string.split_bill_list_shares
                },
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (editor.mode == SplitMode.EQUAL) {
            TextButton(onClick = { onEvent(SplitBillUiEvent.OnEveryoneToggled) }) {
                Text(
                    stringResource(
                        if (editor.isEveryoneIncluded) {
                            R.string.split_bill_uncheck_all
                        } else {
                            R.string.split_bill_check_all
                        },
                    ),
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        editor.rows.forEachIndexed { index, row ->
            SplitPersonRow(
                row = row,
                mode = editor.mode,
                isIn = row.person.id in includedIds,
                paidLabel = when {
                    row.person.id !in editor.payerIds -> null
                    hasSeveralPayers -> R.string.split_bill_row_paid_part
                    else -> R.string.split_bill_row_paid
                },
                isWithoutValue = row.person.id in withoutValueIds,
                isNobodyIn = isNobodyIn,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) outerRadius else innerRadius,
                    topEnd = if (index == 0) outerRadius else innerRadius,
                    bottomStart = if (index == editor.rows.lastIndex) outerRadius else innerRadius,
                    bottomEnd = if (index == editor.rows.lastIndex) outerRadius else innerRadius,
                ),
                minHeight = rowMinHeight,
                money = money,
                onEvent = onEvent,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SplitEditorEqualPreview() {
    PreviewSplitEditorContent(createPreviewEqualEditor(isAnyoneIn = true))
}

@DevicePreviews
@Composable
private fun SplitEditorNonePreview() {
    PreviewSplitEditorContent(createPreviewEqualEditor(isAnyoneIn = false))
}

@DevicePreviews
@Composable
private fun SplitEditorAmountErrorPreview() {
    PreviewSplitEditorContent(createPreviewAmountEditor())
}

@DevicePreviews
@Composable
private fun SplitEditorPercentPreview() {
    PreviewSplitEditorContent(createPreviewPercentEditor())
}

@DevicePreviews
@Composable
private fun SplitEditorSharesPreview() {
    PreviewSplitEditorContent(createPreviewSharesEditor())
}

@Composable
private fun PreviewSplitEditorContent(editor: SplitEditor) {
    BlitzSplitTheme {
        SplitEditorContent(
            state = createPreviewBillState(
                description = if (editor.mode == SplitMode.AMOUNT) "Jantar no Ostradamus" else "Mercado",
                splitEditor = editor,
            ),
            editor = editor,
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

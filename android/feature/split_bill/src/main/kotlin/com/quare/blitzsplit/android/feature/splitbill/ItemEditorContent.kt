package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.layout.FormIconAction
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemPersonLine
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

private const val DIMMED_ALPHA = 0.5f
private const val PERCENT_SUFFIX = "%"
private val percentFieldWidth = 84.dp

/*
 * Why: "Item" (7.6) over "Itens da conta": name, quantity and price with the item's total and its modes on one side,
 * who consumed it on the other. "Apagar item" asks first; a new item has nothing to delete yet.
 */
@Composable
internal fun ItemEditorContent(
    editor: ItemEditor,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(editor.currency)
    val rowMinHeight = if (rememberDeviceLayout().isLandscape && !isDialog) 52.dp else 60.dp
    val deleteLabel = stringResource(R.string.split_bill_item_delete)
    val itemId = editor.itemId
    ColumnsFormLayout(
        chrome = FormChrome(
            title = stringResource(R.string.split_bill_item_title),
            subtitle = null,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.split_bill_back),
            onNavigate = { onEvent(SplitBillUiEvent.OnItemDismissed) },
            confirmLabel = stringResource(R.string.split_bill_done),
            isConfirmEnabled = editor.canSave,
            onConfirm = { onEvent(SplitBillUiEvent.OnItemDone) },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = itemId?.let {
                FormIconAction(
                    icon = Icons.Outlined.Delete,
                    label = deleteLabel,
                    onClick = { onEvent(SplitBillUiEvent.OnItemDeleteClicked(it)) },
                )
            },
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = { onEvent(SplitBillUiEvent.OnItemNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.split_bill_item_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountStepperRow(
                    count = editor.quantity,
                    label = stringResource(R.string.split_bill_item_quantity),
                    decreaseLabel = stringResource(R.string.split_bill_item_less),
                    increaseLabel = stringResource(R.string.split_bill_item_more),
                    canDecrease = editor.canDecreaseQuantity,
                    canIncrease = editor.canIncreaseQuantity,
                    isError = false,
                    onDecrease = { onEvent(SplitBillUiEvent.OnItemQuantityDecreased) },
                    onIncrease = { onEvent(SplitBillUiEvent.OnItemQuantityIncreased) },
                )
                OutlinedTextField(
                    value = editor.unitPriceText,
                    onValueChange = { onEvent(SplitBillUiEvent.OnItemPriceChanged(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.split_bill_item_price)) },
                    prefix = { Text(money.symbol + " ") },
                    placeholder = { Text(money.zeroPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.split_bill_item_total),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = money.toText(editor.totalMinor),
                    fontWeight = FontWeight.Bold,
                )
            }
            ConnectedButtonRow(
                labels = editor.modes.map { itemModeLabelOf(it) },
                selectedIndex = editor.modes.indexOf(editor.mode),
                groupLabel = stringResource(R.string.split_bill_item_modes),
                onClick = { index -> onEvent(SplitBillUiEvent.OnItemModeClicked(editor.modes[index])) },
                itemRole = Role.RadioButton,
                height = 44.dp,
                innerRadius = 6.dp,
                showsCheck = false,
            )
            ItemStatusCard(
                editor = editor,
                money = money,
            )
        },
        second = {
            Text(
                text = itemListLabelOf(editor.mode),
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 8.dp,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                editor.lines.forEachIndexed { index, line ->
                    ItemPersonRow(
                        line = line,
                        editor = editor,
                        index = index,
                        minHeight = rowMinHeight,
                        money = money,
                        onEvent = onEvent,
                    )
                }
            }
            Text(
                text = itemHintOf(editor.mode, money),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun ItemStatusCard(
    editor: ItemEditor,
    money: MoneyFormatter,
) {
    val isOk = !editor.isStatusError
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        color = if (isOk) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (isOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = itemStatusTitleOf(editor.status, money),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            editor.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
            Text(
                text = itemStatusDetailOf(editor.status, money),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Why: one person of the item: Igual's checkbox, Unidades' and Partes' stepper, or Valor's and %'s field.
@Composable
private fun ItemPersonRow(
    line: ItemPersonLine,
    editor: ItemEditor,
    index: Int,
    minHeight: Dp,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val row = line.row
    val name = personNameOf(row.person)
    val isIn = line.shareMinor != null || editor.entries.any { it.memberId == row.person.id && it.value > 0 }
    val toggle = if (editor.mode == ItemSplitMode.EQUAL) {
        Modifier.toggleable(
            value = row.isIncluded,
            role = Role.Checkbox,
            onValueChange = { onEvent(SplitBillUiEvent.OnItemPersonToggled(row.person.id)) },
        )
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(toggle),
        shape = groupShapeOf(index, editor.lines.size),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(
                    start = if (editor.mode == ItemSplitMode.EQUAL) 4.dp else 16.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editor.mode == ItemSplitMode.EQUAL) {
                Checkbox(
                    checked = row.isIncluded,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 8.dp),
                    colors = if (editor.isStatusError) {
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
                modifier = Modifier.alpha(if (isIn) 1f else DIMMED_ALPHA),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isIn || line.isError) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = line.shareMinor?.let(money::toText) ?: stringResource(R.string.split_bill_no_share),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (line.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            ItemPersonInputField(
                line = line,
                mode = editor.mode,
                name = name,
                money = money,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ItemPersonInputField(
    line: ItemPersonLine,
    mode: ItemSplitMode,
    name: String,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val memberId = line.row.person.id
    when (mode) {
        ItemSplitMode.EQUAL -> Unit

        ItemSplitMode.UNITS, ItemSplitMode.SHARES -> CountStepperRow(
            count = line.row.count,
            label = stringResource(
                if (mode ==
                    ItemSplitMode.UNITS
                ) {
                    R.string.split_bill_item_person_units
                } else {
                    R.string.split_bill_person_parts
                },
                name,
            ),
            decreaseLabel = stringResource(R.string.split_bill_item_less),
            increaseLabel = stringResource(R.string.split_bill_item_more),
            canDecrease = line.row.count > 0,
            canIncrease = true,
            isError = line.isError,
            onDecrease = { onEvent(SplitBillUiEvent.OnItemPersonDecreased(memberId)) },
            onIncrease = { onEvent(SplitBillUiEvent.OnItemPersonIncreased(memberId)) },
        )

        ItemSplitMode.AMOUNT -> CompactNumberField(
            value = line.row.valueText,
            onValueChange = { onEvent(SplitBillUiEvent.OnItemPersonValueChanged(memberId, it)) },
            label = stringResource(R.string.split_bill_person_amount, name),
            placeholder = money.zeroPlaceholder,
            prefix = money.symbol,
            suffix = null,
            keyboardType = KeyboardType.Decimal,
            isError = line.isError,
            width = amountFieldWidth,
        )

        ItemSplitMode.PERCENT -> CompactNumberField(
            value = line.row.valueText,
            onValueChange = { onEvent(SplitBillUiEvent.OnItemPersonValueChanged(memberId, it)) },
            label = stringResource(R.string.split_bill_person_percent, name),
            placeholder = "0",
            prefix = null,
            suffix = PERCENT_SUFFIX,
            keyboardType = KeyboardType.Number,
            isError = line.isError,
            width = percentFieldWidth,
        )
    }
}

@DevicePreviews
@Composable
private fun ItemEditorUnitsPreview() {
    PreviewItemEditorContent(ItemSplitMode.UNITS)
}

@DevicePreviews
@Composable
private fun ItemEditorAmountPreview() {
    PreviewItemEditorContent(ItemSplitMode.AMOUNT)
}

@Composable
private fun PreviewItemEditorContent(mode: ItemSplitMode) {
    BlitzSplitTheme {
        ItemEditorContent(
            editor = requireNotNull(createPreviewItemState(mode).itemEditor),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

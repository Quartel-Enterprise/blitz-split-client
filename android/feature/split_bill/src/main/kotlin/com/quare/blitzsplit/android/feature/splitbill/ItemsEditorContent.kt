package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.ColumnsFormLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormChrome
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsItemRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsPersonRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private val outerRadius = 20.dp
private val innerRadius = 6.dp

/*
 * Why: "Itens da conta" (7.5), Dividir's fifth mode in its place: what closes and what each one pays on one side, the
 * items, the service and the couvert on the other. A phone, lying down and the tabletop show each one's total as a
 * strip of chips, a tablet's dialog as a list.
 */
@Composable
internal fun ItemsEditorContent(
    state: SplitBillUiState,
    items: ItemsEditor,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(state.currency)
    val layout = rememberDeviceLayout()
    val rowMinHeight = if (layout.isLandscape && !isDialog) 52.dp else 60.dp
    ColumnsFormLayout(
        chrome = FormChrome(
            title = stringResource(R.string.split_bill_items_title),
            subtitle = null,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationLabel = stringResource(R.string.split_bill_back),
            onNavigate = { onEvent(SplitBillUiEvent.OnItemsDismissed) },
            confirmLabel = stringResource(R.string.split_bill_done),
            isConfirmEnabled = items.addsUp,
            onConfirm = { onEvent(SplitBillUiEvent.OnItemsDone) },
            cancelLabel = stringResource(R.string.split_bill_cancel),
            isNavigationAtEndInDialog = false,
            iconAction = null,
        ),
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        first = {
            SplitSummaryRow(
                state = state,
                totalMinor = items.totalMinor,
                icon = Icons.Outlined.Restaurant,
                money = money,
            )
            ItemsStatusCard(
                items = items,
                money = money,
                onEvent = onEvent,
            )
            if (state.showsOfflineNotice) {
                OfflineNoteRow(text = stringResource(R.string.split_bill_items_offline))
            }
            SectionLabel(stringResource(R.string.split_bill_items_people))
            if (isDialog) {
                PeopleTotalsList(
                    rows = items.personRows,
                    money = money,
                )
            } else {
                PeopleChipsRow(
                    rows = items.personRows,
                    wraps = layout.isLandscape,
                    money = money,
                )
            }
        },
        second = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    text = stringResource(R.string.split_bill_items_count, items.items.size),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onEvent(SplitBillUiEvent.OnAddItemClicked) },
                    enabled = items.canAddItem,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.split_bill_items_add),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.rows.forEachIndexed { index, row ->
                    ItemListRow(
                        row = row,
                        shape = groupShapeOf(index, items.rows.size),
                        minHeight = rowMinHeight,
                        money = money,
                        onClick = { onEvent(SplitBillUiEvent.OnItemClicked(row.item.id)) },
                    )
                }
            }
            SectionLabel(stringResource(R.string.split_bill_items_fees))
            FeesColumn(
                items = items,
                minHeight = rowMinHeight,
                money = money,
                onEvent = onEvent,
            )
        },
    )
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Why: whether it all closes, with 7.5d's two ways out while the items fall short or go over.
@Composable
private fun ItemsStatusCard(
    items: ItemsEditor,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val status = items.status
    StatusCard(
        isOk = items.addsUp,
        title = itemsStatusTitleOf(status, money),
        message = itemsStatusDetailOf(status, money),
        progress = null,
    ) {
        if (status is ItemsStatus.AmountLeft || status is ItemsStatus.AmountOver) {
            DifferenceActionsRow(
                items = items,
                money = money,
                onEvent = onEvent,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifferenceActionsRow(
    items: ItemsEditor,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (items.canAddDifferenceItem) {
            Button(
                onClick = { onEvent(SplitBillUiEvent.OnAddDifferenceItemClicked) },
                colors = colors,
            ) {
                Text(stringResource(R.string.split_bill_items_add_difference, money.toText(items.remainingMinor)))
            }
        }
        Button(
            onClick = { onEvent(SplitBillUiEvent.OnSplitDifferenceClicked) },
            enabled = items.canSplitDifference,
            colors = colors,
        ) {
            Text(stringResource(R.string.split_bill_items_split_difference))
        }
    }
}

// Why: one chip per person, the name over the total; a phone scrolls them sideways, lying down they wrap.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleChipsRow(
    rows: List<ItemsPersonRow>,
    wraps: Boolean,
    money: MoneyFormatter,
) {
    val chips: @Composable () -> Unit = {
        rows.forEach { row ->
            PersonTotalChip(
                row = row,
                money = money,
            )
        }
    }
    if (wraps) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    } else {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    }
}

@Composable
private fun PersonTotalChip(
    row: ItemsPersonRow,
    money: MoneyFormatter,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(
                    start = 8.dp,
                    end = 14.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = row.person.initials,
                color = row.person.avatarColor,
                size = 32.dp,
            )
            Column {
                Text(
                    text = personNameOf(row.person),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = money.toText(row.totalMinor),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PeopleTotalsList(
    rows: List<ItemsPersonRow>,
    money: MoneyFormatter,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, row ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = groupShapeOf(index, rows.size),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .padding(
                            start = 12.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBadge(
                        initial = row.person.initials,
                        color = row.person.avatarColor,
                        size = 36.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = personNameOf(row.person),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = itemsPersonSubtitleOf(row, money),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = money.toText(row.totalMinor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// Why: an item opens "Item"; one nobody is marked in shows in red (7.5c).
@Composable
private fun ItemListRow(
    row: ItemsItemRow,
    shape: RoundedCornerShape,
    minHeight: Dp,
    money: MoneyFormatter,
    onClick: () -> Unit,
) {
    val errorBorder = if (row.isLoose) {
        Modifier.border(
            BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            shape,
        )
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(errorBorder)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = itemRowSubtitleOf(row, money),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.isLoose) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ConsumerFacesRow(row.consumers)
            Text(
                text = money.toText(row.item.totalMinor),
                modifier = Modifier.width(84.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
    }
}

internal fun groupShapeOf(
    index: Int,
    count: Int,
): RoundedCornerShape = RoundedCornerShape(
    topStart = if (index == 0) outerRadius else innerRadius,
    topEnd = if (index == 0) outerRadius else innerRadius,
    bottomStart = if (index == count - 1) outerRadius else innerRadius,
    bottomEnd = if (index == count - 1) outerRadius else innerRadius,
)

@DevicePreviews
@Composable
private fun ItemsEditorPreview() {
    PreviewItemsEditorContent(createPreviewItemsState(hasOwners = true))
}

@DevicePreviews
@Composable
private fun ItemsEditorFreshPreview() {
    PreviewItemsEditorContent(createPreviewItemsState(hasOwners = false))
}

@Composable
private fun PreviewItemsEditorContent(state: SplitBillUiState) {
    BlitzSplitTheme {
        ItemsEditorContent(
            state = state,
            items = requireNotNull(state.itemsEditor),
            isDialog = false,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

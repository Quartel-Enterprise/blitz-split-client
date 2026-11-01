package com.quare.blitzsplit.android.feature.groupdetail

import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

internal const val META_SEPARATOR = " · "

// Why: each day under "Hoje", "Ontem" or its date, its bills and payments read as one card.
internal fun LazyListScope.expenseDayItems(
    days: List<ExpenseDay>,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    days.forEach { day ->
        item(key = "day-${day.dayStartMillis}") {
            Text(
                text = when (day.relativeDay) {
                    RelativeDay.TODAY -> stringResource(R.string.group_detail_today)
                    RelativeDay.YESTERDAY -> stringResource(R.string.group_detail_yesterday)
                    RelativeDay.OTHER -> dates.toWeekdayDateText(day.dayStartMillis)
                },
                modifier = Modifier
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 8.dp,
                    ).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        itemsIndexed(
            items = day.entries,
            key = { _, entry -> entry.id },
        ) { index, entry ->
            val shape = groupedRowShapeOf(
                index = index,
                count = day.entries.size,
            )
            val rowModifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 2.dp,
            )
            when (entry) {
                is ExpenseRow -> ExpenseEntryRow(
                    expense = entry,
                    shape = shape,
                    money = money,
                    dates = dates,
                    onEvent = onEvent,
                    modifier = rowModifier,
                )

                is PaymentRow -> PaymentEntryRow(
                    payment = entry,
                    shape = shape,
                    money = money,
                    dates = dates,
                    onEvent = onEvent,
                    modifier = rowModifier,
                )
            }
        }
    }
}

@Composable
private fun ExpenseEntryRow(
    expense: ExpenseRow,
    shape: Shape,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryFrameRow(
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        leading = { CategoryBadge(category = expense.category) },
        title = expense.description,
        shape = shape,
        onClick = { onEvent(GroupDetailUiEvent.OnExpenseClicked(expense.id)) },
        modifier = modifier,
        meta = {
            ExpenseMetaLine(
                expense = expense,
                dates = dates,
            )
        },
    ) {
        Text(
            text = money.toText(expense.totalMinor),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        expense.myBalanceMinor?.let { balance ->
            Text(
                text = money.toSignedText(balance),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    balance > 0 -> MaterialTheme.colorScheme.primary
                    balance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

// Why: a bill and a payment share the line: the icon, what it was, the meta under it and the amounts at the end.
@Composable
internal fun EntryFrameRow(
    icon: ImageVector,
    title: String,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier,
    meta: @Composable () -> Unit,
    // Why: a bill's category badge in place of the icon (6.1); a payment keeps its icon.
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading() else EntryIconTile(icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                meta()
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = trailing,
            )
        }
    }
}

// Why: "17:40 · na fila · Você pagou", with the home's pending dot while the bill waits for the server.
@Composable
private fun ExpenseMetaLine(
    expense: ExpenseRow,
    dates: DateTimeTextFormatter,
) {
    EntryMetaLine(
        isPending = expense.isPending,
        parts = listOfNotNull(
            dates.toTimeText(expense.spentAt),
            stringResource(R.string.group_detail_queued).takeIf { expense.isPending },
            paidTextOrNull(
                expense.payers,
                isMe = { it.isMe },
                nameOf = { it.name },
            ),
        ),
    )
}

// Why: a bill's or a payment's icon, in the same tile.
@Composable
private fun EntryIconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

// Why: the meta line of a bill or a payment, with the home's pending dot while it waits for the server.
@Composable
internal fun EntryMetaLine(
    isPending: Boolean,
    parts: List<String>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPending) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
        Text(
            text = parts.joinToString(META_SEPARATOR),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

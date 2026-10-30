package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent

private const val META_SEPARATOR = " · "
private val outerRadius = 20.dp
private val innerRadius = 6.dp

// Why: each day under "Hoje", "Ontem" or its date, its bills read as one card.
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
            items = day.expenses,
            key = { _, expense -> expense.id },
        ) { index, expense ->
            ExpenseEntryRow(
                expense = expense,
                shape = rowShapeOf(
                    index = index,
                    count = day.expenses.size,
                ),
                money = money,
                dates = dates,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 2.dp,
                ),
            )
        }
    }
}

private fun rowShapeOf(
    index: Int,
    count: Int,
): Shape = RoundedCornerShape(
    topStart = if (index == 0) outerRadius else innerRadius,
    topEnd = if (index == 0) outerRadius else innerRadius,
    bottomStart = if (index == count - 1) outerRadius else innerRadius,
    bottomEnd = if (index == count - 1) outerRadius else innerRadius,
)

@Composable
private fun ExpenseEntryRow(
    expense: ExpenseRow,
    shape: Shape,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onEvent(GroupDetailUiEvent.OnExpenseClicked(expense.id)) },
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ExpenseMetaLine(
                    expense = expense,
                    dates = dates,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
    }
}

// Why: "17:40 · na fila · Você pagou", with the home's pending dot while the bill waits for the server.
@Composable
private fun ExpenseMetaLine(
    expense: ExpenseRow,
    dates: DateTimeTextFormatter,
) {
    val parts = listOfNotNull(
        dates.toTimeText(expense.spentAt),
        stringResource(R.string.group_detail_queued).takeIf { expense.isPending },
        when {
            expense.payerIsMe -> stringResource(R.string.group_detail_you_paid)
            expense.payerName != null -> stringResource(R.string.group_detail_someone_paid, expense.payerName.orEmpty())
            else -> null
        },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expense.isPending) {
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

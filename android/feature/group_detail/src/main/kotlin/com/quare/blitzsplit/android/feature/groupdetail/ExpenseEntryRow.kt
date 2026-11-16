package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.BillAmountsColumn
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.ENTRY_META_SEPARATOR
import com.quare.blitzsplit.android.designsystem.component.EntryDayHeading
import com.quare.blitzsplit.android.designsystem.component.EntryIconTile
import com.quare.blitzsplit.android.designsystem.component.EntryMetaLine
import com.quare.blitzsplit.android.designsystem.component.LocalBillOpensAsDialog
import com.quare.blitzsplit.android.designsystem.component.MenuEntryLine
import com.quare.blitzsplit.android.designsystem.component.RowMenuItem
import com.quare.blitzsplit.android.designsystem.component.SharedOrigin
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.rememberBillSharedModifiers
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

internal const val META_SEPARATOR = ENTRY_META_SEPARATOR

// Why: each day under "Hoje", "Ontem" or its date, its bills and payments read as one card.
internal fun LazyListScope.expenseDayItems(
    days: List<ExpenseDay>,
    dates: DateTimeTextFormatter,
    showsSeeAll: Boolean,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    days.forEachIndexed { index, day ->
        item(
            key = "day-${day.dayStartMillis}",
            contentType = "day",
        ) {
            val heading = when (day.relativeDay) {
                RelativeDay.TODAY -> stringResource(R.string.group_detail_today)
                RelativeDay.YESTERDAY -> stringResource(R.string.group_detail_yesterday)
                RelativeDay.OTHER -> dates.toWeekdayDateText(day.dayStartMillis)
            }
            // Why: "Ver tudo" opens "Todas as despesas" filtered by this group, at the end of the list's first heading.
            if (index == 0 && showsSeeAll) {
                EntryDayHeading(
                    text = heading,
                    action = stringResource(R.string.group_detail_see_all_bills),
                    onAction = { onEvent(GroupDetailUiEvent.OnSeeAllBillsClicked) },
                )
            } else {
                EntryDayHeading(text = heading)
            }
        }
        itemsIndexed(
            items = day.entries,
            key = { _, entry -> entry.id },
            contentType = { _, entry -> entry::class },
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
                    dates = dates,
                    onEvent = onEvent,
                    modifier = rowModifier,
                )

                is PaymentRow -> PaymentEntryRow(
                    payment = entry,
                    shape = shape,
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
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why: the bill's own currency, not the one picked above the balances.
    val money = rememberMoneyFormatter(expense.currency)
    /*
     * Why: the badge, title, total and share fly into the bill's screen as Home's rows do (beta feedback of 2026-11-15),
     * and the time into its date and time (beta feedback of 2026-11-16).
     */
    val shared = rememberBillSharedModifiers(
        billId = expense.id,
        origin = SharedOrigin.GROUP.takeUnless { LocalBillOpensAsDialog.current },
    )
    EntryFrameRow(
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        leading = {
            CategoryBadge(
                category = expense.category,
                modifier = shared.category,
            )
        },
        title = expense.description,
        titleModifier = shared.title,
        shape = shape,
        onClick = { onEvent(GroupDetailUiEvent.OnExpenseClicked(expense.id)) },
        menuItems = expenseMenuItemsOf(expense.id, onEvent),
        modifier = modifier,
        meta = {
            ExpenseMetaLine(
                expense = expense,
                dates = dates,
                timeModifier = shared.time,
            )
        },
    ) {
        BillAmountsColumn(
            totalMinor = expense.totalMinor,
            myBalanceMinor = expense.myBalanceMinor,
            money = money,
            totalModifier = shared.total,
            shareModifier = shared.share,
        )
    }
}

// Why: a bill and a payment share the line, ending in its ⋮, which a long press on the line opens too.
@Composable
internal fun EntryFrameRow(
    icon: ImageVector,
    title: String,
    shape: Shape,
    onClick: () -> Unit,
    menuItems: List<RowMenuItem>,
    modifier: Modifier,
    meta: @Composable () -> Unit,
    // Why: a bill's category badge in place of the icon (6.1); a payment keeps its icon.
    leading: (@Composable () -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    trailing: @Composable ColumnScope.() -> Unit,
) {
    MenuEntryLine(
        title = title,
        shape = shape,
        onClick = onClick,
        menuItems = menuItems,
        modifier = modifier,
        leading = leading ?: { EntryIconTile(icon) },
        meta = meta,
        titleModifier = titleModifier,
        trailing = trailing,
    )
}

// Why: "17:40 · na fila · Você pagou", with the home's pending dot while the bill waits for the server.
@Composable
private fun ExpenseMetaLine(
    expense: ExpenseRow,
    dates: DateTimeTextFormatter,
    timeModifier: Modifier,
) {
    EntryMetaLine(
        isPending = expense.isPending,
        timeModifier = timeModifier,
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

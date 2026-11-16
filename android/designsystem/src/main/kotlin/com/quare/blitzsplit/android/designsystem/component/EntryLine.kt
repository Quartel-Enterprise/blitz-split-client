package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter

const val ENTRY_META_SEPARATOR = " · "

/*
 * Why: a line of a list of bills (the group's Expenses, a category's in the statistics): the icon, what it was, the
 * meta under it, the amounts and, where the list has one, its ⋮ at the end, which a long press opens too.
 */
@Composable
fun EntryLineSurface(
    title: String,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier,
    leading: @Composable () -> Unit,
    meta: @Composable () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    // Why: the line's ⋮, after the amounts; a list without one ends the line on them.
    end: (@Composable () -> Unit)? = null,
    // Why: the title's shared bounds, which fly into the bill's screen; none for a payment.
    titleModifier: Modifier = Modifier,
    trailing: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = onLongClickLabel,
                ).padding(
                    start = 16.dp,
                    end = if (end != null) 4.dp else 16.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    modifier = titleModifier,
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
            end?.invoke()
        }
    }
}

/*
 * Why: a line ending in its ⋮, which a long press on the line opens too: the group's Expenses and the statistics'
 * "Expenses by category" (beta feedback) draw their bills with it, so both offer the same options the same way.
 */
@Composable
fun MenuEntryLine(
    title: String,
    shape: Shape,
    onClick: () -> Unit,
    menuItems: List<RowMenuItem>,
    modifier: Modifier,
    leading: @Composable () -> Unit,
    meta: @Composable () -> Unit,
    titleModifier: Modifier = Modifier,
    trailing: @Composable ColumnScope.() -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    val optionsLabel = rowOptionsLabelOf(title)
    EntryLineSurface(
        title = title,
        shape = shape,
        onClick = onClick,
        modifier = modifier,
        leading = leading,
        meta = meta,
        onLongClick = { isMenuOpen = true },
        onLongClickLabel = optionsLabel,
        titleModifier = titleModifier,
        end = {
            RowMenuButton(
                contentDescription = optionsLabel,
                items = menuItems,
                isOpen = isMenuOpen,
                onOpenChange = { isMenuOpen = it },
            )
        },
        trailing = trailing,
    )
}

// Why: a payment's icon (a bill shows its category's badge instead), in the badge's size.
@Composable
fun EntryIconTile(icon: ImageVector) {
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

/*
 * Why: "17:40 · queued · You paid", with the home's pending dot while the line waits for the server. With a
 * timeModifier the first part, the time, is a text of its own, which flies into the bill's date and time (beta
 * feedback of 2026-11-16).
 */
@Composable
fun EntryMetaLine(
    isPending: Boolean,
    parts: List<String>,
    timeModifier: Modifier? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPending) {
            PendingIndicator()
        }
        val time = parts.firstOrNull().takeIf { timeModifier != null }
        Row {
            if (time != null && timeModifier != null) {
                EntryMetaText(
                    text = time,
                    modifier = timeModifier,
                )
            }
            val rest = if (time != null) parts.drop(1) else parts
            if (rest.isNotEmpty()) {
                EntryMetaText(
                    text = rest.joinToString(
                        separator = ENTRY_META_SEPARATOR,
                        prefix = if (time != null) ENTRY_META_SEPARATOR else "",
                    ),
                )
            }
        }
    }
}

@Composable
private fun EntryMetaText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// Why: a bill's total, and under it what it does to this device's user's balance ("+R$ 600,00"), when they are in it.
@Composable
fun BillAmountsColumn(
    totalMinor: Long,
    myBalanceMinor: Long?,
    money: MoneyFormatter,
    // Why: the total's and the share's shared bounds, which fly into the bill's screen.
    totalModifier: Modifier = Modifier,
    shareModifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = money.toText(totalMinor),
            modifier = totalModifier,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        myBalanceMinor?.let { balance ->
            Text(
                text = money.toSignedText(balance),
                modifier = shareModifier,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColorOf(balance),
            )
        }
    }
}

// Why: "Today", "Yesterday" or the date over a day's lines, read as a heading.
@Composable
fun EntryDayHeading(text: String) {
    DayHeadingText(
        text = text,
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 8.dp,
        ),
    )
}

/*
 * Why: the first day's heading of the group's Despesas with "Ver tudo" at its end, as Início's "Últimas despesas" draws
 * its link; the button's own height takes the heading's top margin, so the rows start where they would without it.
 */
@Composable
fun EntryDayHeading(
    text: String,
    action: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 8.dp,
                top = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayHeadingText(
            text = text,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) { Text(action) }
    }
}

// Why: the day's words as every heading draws them, so the one with "Ver tudo" never looks unlike the others.
@Composable
private fun DayHeadingText(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

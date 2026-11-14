package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.AmountText
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.RowMenuButton
import com.quare.blitzsplit.android.designsystem.component.TappableCard
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.android.designsystem.component.billMenuItemsOf
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.rememberGroupSharedModifiers
import com.quare.blitzsplit.android.designsystem.component.rowOptionsLabelOf
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState

// Why: the cards of "Seus grupos"; the group's square and the payers' faces under a bill, a line of text tall.
private val groupCardShape = RoundedCornerShape(24.dp)
private val billOriginSize = 20.dp

// Why: "Seus grupos", up to three cards in a row: the pinned first, then the latest activity.
@Composable
internal fun HomeGroupsSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (!state.isLoaded || (state.groups.isEmpty() && state.isFirstDownload)) {
        HomeGroupsSkeleton(metrics)
        return
    }
    if (state.groups.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.groups_home_groups_title),
            action = stringResource(R.string.groups_home_see_all_groups),
            onAction = { onEvent(HomeUiEvent.OnSeeAllGroupsClicked) },
            modifier = Modifier.padding(horizontal = metrics.gutter),
        )
        // Why: a larger font makes a card taller than its board's height, and every card takes the tallest one's.
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = metrics.gutter)
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.groups.forEach { group ->
                HomeGroupCard(
                    group = group,
                    metrics = metrics,
                    onClick = { onEvent(HomeUiEvent.OnGroupClicked(group.row.id)) },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

/*
 * Why: a group's card: its shape, the pin "Fixar na Início" left, its name, what Grupos' row says under it (the last
 * activity, "na fila · Mercado") and the balance with its word.
 */
@Composable
private fun HomeGroupCard(
    group: HomeGroup,
    metrics: HomeMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val row = group.row
    val money = rememberMoneyFormatter(row.currency)
    val dates = rememberDateTimeTextFormatter()
    val amount = money.toSignedText(row.netMinor)
    val word = stringResource(balanceLabelOf(row.balanceKind))
    val shared = rememberGroupSharedModifiers(row.id)
    val description = stringResource(
        if (group.isPinned) R.string.groups_home_pinned_group_description else R.string.groups_home_group_description,
        row.name,
        amount,
        word,
    )
    TappableCard(
        onClick = onClick,
        modifier = modifier
            .width(metrics.groupCardWidth)
            .heightIn(min = metrics.groupCardHeight)
            .semantics { contentDescription = description },
        shape = groupCardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
                .padding(
                    horizontal = if (metrics.isCompact) 14.dp else 16.dp,
                    vertical = if (metrics.isCompact) 12.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                GroupBadge(
                    initials = row.initials,
                    color = row.color,
                    photoUrl = row.photoUrl,
                    size = if (metrics.isCompact) 36.dp else 40.dp,
                    modifier = shared.avatar,
                )
                if (group.isPinned) {
                    Box(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = row.name,
                    modifier = shared.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = if (metrics.isCompact) 14.sp else 15.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (row.isPending) PendingIndicator()
                    Text(
                        text = detailTextOf(
                            group = row,
                            amountText = money::toAmountText,
                            dates = dates,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                AmountText(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (metrics.isCompact) 16.sp else 17.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = amountColorOf(row.netMinor),
                )
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/*
 * Why: "Últimas despesas", the five latest bills you paid or take part in, by day, the payments left to Atividade.
 * While Room is read, or the first download fills the device, a shimmer holds their place.
 */
@Composable
internal fun HomeBillsSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (!state.isLoaded) {
        HomeBillsSkeleton(modifier = Modifier.padding(horizontal = metrics.gutter))
        return
    }
    if (state.billDays.isEmpty() && !state.isFirstDownload) return
    Column(modifier = Modifier.padding(horizontal = metrics.gutter)) {
        HomeSectionHeader(
            title = stringResource(R.string.groups_home_bills_title),
            action = stringResource(R.string.groups_home_see_all_bills),
            onAction = { onEvent(HomeUiEvent.OnSeeAllBillsClicked) },
        )
        state.billDays.forEach { day ->
            BillDayColumn(
                day = day,
                metrics = metrics,
                onEvent = onEvent,
            )
        }
        if (state.isFirstDownload) HomeBillsSkeleton()
    }
}

@Composable
private fun BillDayColumn(
    day: HomeBillDay,
    metrics: HomeMetrics,
    onEvent: (HomeUiEvent) -> Unit,
) {
    HomeBillDayHeading(day)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        day.bills.forEachIndexed { index, bill ->
            HomeBillRow(
                bill = bill,
                shape = groupedRowShapeOf(index, day.bills.size),
                isCompact = metrics.isCompact,
                onClick = { onEvent(HomeUiEvent.OnBillClicked(bill.groupId, bill.id)) },
                onMenuEvent = onEvent,
            )
        }
    }
}

// Why: "Hoje", "Ontem" or the date over a day's bills, on Início and on "Todas as despesas".
@Composable
internal fun HomeBillDayHeading(day: HomeBillDay) {
    val dates = rememberDateTimeTextFormatter()
    Text(
        text = when (day.relativeDay) {
            RelativeDay.TODAY -> stringResource(R.string.groups_home_today)
            RelativeDay.YESTERDAY -> stringResource(R.string.groups_home_yesterday)
            RelativeDay.OTHER -> dates.toWeekdayDateText(day.dayStartMillis)
        },
        modifier = Modifier
            .padding(
                horizontal = 4.dp,
                vertical = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/*
 * Why: a bill: its category's badge, "Mercado", the group's square and the faces of who paid (beta feedback, as the
 * legacy app drew them: the line "Viagem Floripa · Você pagou" cut off on a phone; TalkBack still reads it), the amber
 * dot and "na fila" while it waits to go up, its total and what it does to your balance. With onMenuEvent it ends in the
 * group's ⋮ (Pierre's call, beta feedback), which a long press on the row opens too; the cover screen has none.
 */
@Composable
internal fun HomeBillRow(
    bill: HomeBill,
    shape: Shape,
    isCompact: Boolean,
    onClick: (() -> Unit)?,
    onMenuEvent: ((BillMenuEvent) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var isMenuOpen by rememberSaveable(bill.id) { mutableStateOf(false) }
    val menuItems = onMenuEvent?.let { send ->
        billMenuItemsOf(
            onEdit = { send(BillMenuEvent.OnEditClicked(bill.groupId, bill.id)) },
            onDuplicate = { send(BillMenuEvent.OnDuplicateClicked(bill.groupId, bill.id)) },
            onShare = { send(BillMenuEvent.OnShareClicked(bill.groupId, bill.id)) },
            onDelete = { send(BillMenuEvent.OnDeleteClicked(bill.groupId, bill.id)) },
        )
    }
    val optionsLabel = rowOptionsLabelOf(bill.description)
    val money = rememberMoneyFormatter(bill.currency)
    val paidBy = paidTextOrNull(
        bill.payers,
        isMe = { it.isMe },
        nameOf = { it.name },
    )
    val origin = paidBy?.let { stringResource(R.string.groups_search_bill_meta, bill.groupName, it) } ?: bill.groupName
    val meta = if (bill.isPending) stringResource(R.string.groups_meta_queued_bill, origin) else origin
    val content: @Composable () -> Unit = {
        val padding = if (isCompact) 14.dp else 16.dp
        Row(
            modifier = Modifier.padding(
                start = padding,
                // Why: the ⋮ brings its own touch target's room.
                end = if (menuItems != null) 4.dp else padding,
                top = if (isCompact) 10.dp else 12.dp,
                bottom = if (isCompact) 10.dp else 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Why: as Despesas draws it (6.1).
            CategoryBadge(category = bill.category)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = bill.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BillOriginRow(
                    bill = bill,
                    description = meta,
                )
            }
            Column(
                modifier = Modifier.cappedAtHalfWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = money.toText(bill.totalMinor),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (bill.myBalanceMinor != 0L) {
                    // Why: the sign and the color say it in a list, as Grupos' list does; TalkBack still reads the word.
                    val share = money.toSignedText(bill.myBalanceMinor)
                    val shareLabel = stringResource(
                        R.string.groups_home_amount_with_word,
                        share,
                        stringResource(
                            if (bill.myBalanceMinor > 0) {
                                R.string.groups_balance_to_receive
                            } else {
                                R.string.groups_balance_you_owe
                            },
                        ),
                    )
                    Text(
                        text = share,
                        modifier = Modifier.semantics { contentDescription = shareLabel },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColorOf(bill.myBalanceMinor),
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (menuItems != null) {
                RowMenuButton(
                    contentDescription = optionsLabel,
                    items = menuItems,
                    isOpen = isMenuOpen,
                    onOpenChange = { isMenuOpen = it },
                )
            }
        }
    }
    val rowModifier = modifier.fillMaxWidth()
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    if (onClick != null) {
        // Why: the Surface clips the ripple to the row's shape.
        Surface(
            modifier = rowModifier,
            shape = shape,
            color = color,
        ) {
            Box(
                modifier = Modifier.combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = menuItems?.let { { isMenuOpen = true } },
                    onLongClickLabel = optionsLabel.takeIf { menuItems != null },
                ),
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = rowModifier,
            shape = shape,
            color = color,
            content = content,
        )
    }
}

/*
 * Why: the amounts are measured before the description, with all the room after the badge; at a large font they took
 * it all and the description had none left. Capped at half of it, they end in an ellipsis instead.
 */
private fun Modifier.cappedAtHalfWidth(): Modifier = layout { measurable, constraints ->
    val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth / 2 else constraints.maxWidth
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = 0,
            maxWidth = maxWidth,
        ),
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

// Why: the group's square, then who paid as a stack of faces; nothing here to cut off on a narrow phone.
@Composable
private fun BillOriginRow(
    bill: HomeBill,
    description: String,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupBadge(
            initials = bill.groupInitials,
            color = bill.groupColor,
            photoUrl = bill.groupPhotoUrl,
            size = billOriginSize,
        )
        FacesRow(
            faces = bill.payerFaces,
            extraCount = bill.extraPayerCount,
            size = billOriginSize,
            ringColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        if (bill.isPending) {
            PendingIndicator()
            Text(
                text = stringResource(R.string.groups_meta_queued),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow

private val outerRadius = 20.dp
private val innerRadius = 6.dp
private val payerAvatarSize = 32.dp
private val rowAvatarSize = 40.dp

// Why: what the bill was, when, how much and who paid it, with the pending line while it waits for the server.
@Composable
internal fun BillSummaryColumn(
    state: BillDetailUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = state.description,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = spentAtTextOf(
                spentAt = state.spentAt,
                spentDay = state.spentDay,
                dates = dates,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = money.toText(state.totalMinor),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val payer = state.payer
            if (payer != null) {
                AvatarBadge(
                    initial = payer.initials,
                    color = payer.avatarColor,
                    size = payerAvatarSize,
                )
            } else {
                FormerMemberBadge(payerAvatarSize)
            }
            Text(
                text = payerTextOf(payer),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        if (state.isPending) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                )
                Text(
                    text = stringResource(R.string.bill_detail_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Sua parte", what the bill does to this device's user's balance, and with whom it settles.
@Composable
internal fun MyPartCard(
    state: BillDetailUiState,
    money: MoneyFormatter,
) {
    val balance = state.myBalanceMinor ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_detail_my_part),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = if (balance < 0) money.toText(-balance) else money.toSignedText(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        balance > 0 -> MaterialTheme.colorScheme.primary
                        balance < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = stringResource(
                        when {
                            balance > 0 -> R.string.bill_detail_to_get_back
                            balance < 0 -> R.string.bill_detail_you_owe
                            else -> R.string.bill_detail_nothing_to_settle
                        },
                    ),
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.myPartNote?.let { note ->
                Text(
                    text = myPartNoteTextOf(
                        note = note,
                        money = money,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Igual entre 5" over everyone in the group, each with their part or "—" when out of the split.
@Composable
internal fun BillSplitSection(
    state: BillDetailUiState,
    money: MoneyFormatter,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = splitTitleOf(
                mode = state.splitMode,
                count = state.participantCount,
            ),
            modifier = Modifier
                .padding(
                    start = 4.dp,
                    bottom = 6.dp,
                ).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.people.forEachIndexed { index, row ->
            ShareRow(
                row = row,
                shape = rowShapeOf(
                    index = index,
                    count = state.people.size,
                ),
                money = money,
            )
        }
    }
}

@Composable
private fun ShareRow(
    row: BillShareRow,
    shape: Shape,
    money: MoneyFormatter,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            if (row.name == null) {
                FormerMemberBadge(rowAvatarSize)
            } else {
                AvatarBadge(
                    initial = row.initials,
                    color = row.avatarColor,
                    size = rowAvatarSize,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = rowNameOf(row),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                rowSubtitleOf(
                    row = row,
                    money = money,
                )?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = row.shareMinor?.let(money::toText) ?: stringResource(R.string.bill_detail_no_share),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (row.shareMinor == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// Why: someone who left the group keeps no name nor color, so a plain person icon stands for them.
@Composable
private fun FormerMemberBadge(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

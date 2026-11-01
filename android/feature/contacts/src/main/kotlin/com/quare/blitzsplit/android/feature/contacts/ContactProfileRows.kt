package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.contacts.domain.model.ContactGroup
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistoryItem

private val rowPadding = Modifier.padding(vertical = 1.dp)

// Why: one of "Grupos em comum", with what that group adds to the net; not tappable, as the canvas draws it.
@Composable
internal fun CommonGroupRow(
    group: ContactGroup,
    index: Int,
    count: Int,
) {
    val money = rememberMoneyFormatter(group.currency)
    ProfileRow(
        index = index,
        count = count,
        leading = {
            GroupBadge(
                initials = group.groupInitials,
                color = group.groupColor,
                size = 36.dp,
            )
        },
        title = group.groupName,
        meta = netWordOf(group.amountMinor),
        amount = money.toSignedText(group.amountMinor),
        amountColor = netColorOf(group.amountMinor),
        word = null,
    )
}

/*
 * Why: a bill or a payment between the two, which add up to the net: "Mercado da semana · Apê 302 · 28 out · Carla
 * pagou R$ 196,00 · −R$ 98,00 você deve", "Carla te pagou · Viagem Floripa · 30 out · Pix · R$ 50,00 recebido".
 */
@Composable
internal fun HistoryRow(
    item: ContactHistoryItem,
    name: String,
    index: Int,
    count: Int,
) {
    val money = rememberMoneyFormatter(item.currency)
    val dates = rememberDateTimeTextFormatter()
    when (item) {
        is ContactHistoryItem.Bill -> ProfileRow(
            index = index,
            count = count,
            leading = { HistoryIcon(isPayment = false) },
            title = item.description,
            meta = stringResource(
                R.string.contacts_history_meta,
                item.groupName,
                dates.toDateText(item.at),
                payerTextOf(item, money.toText(item.totalMinor)),
            ),
            amount = money.toSignedText(item.effectMinor),
            amountColor = netColorOf(item.effectMinor),
            word = netWordOf(item.effectMinor),
        )

        is ContactHistoryItem.Payment -> ProfileRow(
            index = index,
            count = count,
            leading = { HistoryIcon(isPayment = true) },
            title = stringResource(
                if (item.isFromMe) R.string.contacts_history_payment_to else R.string.contacts_history_payment_from,
                name,
            ),
            meta = paymentMetaOf(item, dates),
            amount = money.toText(item.amountMinor),
            amountColor = MaterialTheme.colorScheme.onSurface,
            word = stringResource(
                if (item.isFromMe) R.string.contacts_history_paid else R.string.contacts_history_received,
            ),
        )
    }
}

@Composable
private fun paymentMetaOf(
    item: ContactHistoryItem.Payment,
    dates: DateTimeTextFormatter,
): String = stringResource(
    R.string.contacts_history_meta,
    item.groupName,
    dates.toDateText(item.at),
    stringResource(item.method.labelRes),
)

// Why: "Carla pagou R$ 196,00", "você pagou R$ 130,00" or "3 pessoas pagaram".
@Composable
private fun payerTextOf(
    bill: ContactHistoryItem.Bill,
    total: String,
): String {
    val payerName = bill.payerName
    return when {
        bill.isPaidByMe -> stringResource(R.string.contacts_history_paid_by_me, total)
        payerName != null -> stringResource(R.string.contacts_history_paid_by, payerName, total)
        else -> pluralStringResource(R.plurals.contacts_history_payers, bill.payerCount, bill.payerCount)
    }
}

@Composable
private fun HistoryIcon(isPayment: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (isPayment) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPayment) {
                Icons.AutoMirrored.Outlined.ArrowForward
            } else {
                Icons.AutoMirrored.Outlined.ReceiptLong
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isPayment) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ProfileRow(
    index: Int,
    count: Int,
    leading: @Composable () -> Unit,
    title: String,
    meta: String,
    amount: String,
    amountColor: Color,
    word: String?,
) {
    Surface(
        modifier = rowPadding
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1,
                )
                word?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

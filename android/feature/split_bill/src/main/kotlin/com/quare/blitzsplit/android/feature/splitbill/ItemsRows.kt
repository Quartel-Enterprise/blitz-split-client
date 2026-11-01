package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

private const val FACE_COUNT = 3

@Composable
internal fun ConsumerFacesRow(people: List<BillPerson>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        people.take(FACE_COUNT).forEach { person ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AvatarBadge(
                    initial = person.initials,
                    color = person.avatarColor,
                    size = 24.dp,
                )
            }
        }
    }
}

// Why: "Serviço (10%)" and "Couvert", each opening its field, and the difference "Dividir a diferença" shared.
@Composable
internal fun FeesColumn(
    items: ItemsEditor,
    minHeight: Dp,
    money: MoneyFormatter,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val count = if (items.showsExtra) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FeeRow(
            title = stringResource(R.string.split_bill_items_service, items.split.servicePercent),
            subtitle = stringResource(R.string.split_bill_items_service_sub),
            amount = money.toText(items.breakdown.serviceMinor),
            shape = groupShapeOf(0, count),
            minHeight = minHeight,
            onClick = { onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE)) },
        )
        FeeRow(
            title = stringResource(R.string.split_bill_items_couvert),
            subtitle = stringResource(R.string.split_bill_items_couvert_sub, money.toText(items.split.couvertMinor)),
            amount = money.toText(items.breakdown.couvertMinor),
            shape = groupShapeOf(1, count),
            minHeight = minHeight,
            onClick = { onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT)) },
        )
        if (items.showsExtra) {
            FeeRow(
                title = stringResource(R.string.split_bill_items_extra),
                subtitle = stringResource(R.string.split_bill_items_service_sub),
                amount = money.toSignedText(items.split.extraMinor),
                shape = groupShapeOf(2, count),
                minHeight = minHeight,
                onClick = null,
                trailing = {
                    IconButton(onClick = { onEvent(SplitBillUiEvent.OnDifferenceRemoved) }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.split_bill_items_extra_remove),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FeeRow(
    title: String,
    subtitle: String,
    amount: String,
    shape: RoundedCornerShape,
    minHeight: Dp,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(
                    start = 16.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            trailing()
        }
    }
}

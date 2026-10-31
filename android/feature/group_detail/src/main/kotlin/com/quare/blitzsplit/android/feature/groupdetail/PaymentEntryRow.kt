package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

// Why: "Ana te pagou · 12:10 · Acerto · Pix · R$ 50,00 · acerto"; tapping it asks "Apagar este acerto?".
@Composable
internal fun PaymentEntryRow(
    payment: PaymentRow,
    shape: Shape,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryFrameRow(
        icon = Icons.Outlined.Payments,
        title = paymentTitleOf(payment),
        shape = shape,
        onClick = { onEvent(GroupDetailUiEvent.OnPaymentClicked(payment.id)) },
        modifier = modifier,
        meta = {
            EntryMetaLine(
                isPending = payment.isPending,
                parts = listOfNotNull(
                    dates.toTimeText(payment.paidAt),
                    stringResource(R.string.group_detail_queued).takeIf { payment.isPending },
                    stringResource(R.string.group_detail_payment_meta),
                    stringResource(payment.method.labelRes),
                ),
            )
        },
    ) {
        Text(
            text = money.toText(payment.amountMinor),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = stringResource(R.string.group_detail_payment_tag),
                modifier = Modifier.padding(
                    horizontal = 6.dp,
                    vertical = 1.dp,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// Why: "Ana te pagou", "Você pagou Bruno" or "Diego pagou Bruno"; "Ex-membro" for someone no longer in the group.
@Composable
private fun paymentTitleOf(payment: PaymentRow): String {
    val formerMember = stringResource(R.string.group_detail_former_member)
    val from = payment.fromName ?: formerMember
    val to = payment.toName ?: formerMember
    return when {
        payment.toIsMe -> stringResource(R.string.group_detail_payment_paid_you, from)
        payment.fromIsMe -> stringResource(R.string.group_detail_payment_you_paid, to)
        else -> stringResource(R.string.group_detail_payment_paid, from, to)
    }
}

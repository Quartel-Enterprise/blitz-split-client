package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

/*
 * Why: a tap on a payment opens what it holds (who paid whom, how much, when and how) instead of asking to delete it,
 * which scared; Apagar is here for who wants it and still asks "Apagar este acerto?". A bottom sheet on a phone, a
 * dialog on a tablet and a foldable's half.
 */
@Composable
internal fun PaymentDetailsSheet(
    payment: PaymentRow,
    money: MoneyFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
    dates: DateTimeTextFormatter = rememberDateTimeTextFormatter(),
) {
    val title = paymentTitleOf(payment)
    AdaptiveSheet(
        title = title,
        onDismissRequest = { onEvent(GroupDetailUiEvent.OnPaymentDetailsDismissed) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = money.toText(payment.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = listOfNotNull(
                    "${dates.toDateText(payment.paidAt)} · ${dates.toTimeText(payment.paidAt)}",
                    stringResource(R.string.group_detail_queued).takeIf { payment.isPending },
                    stringResource(R.string.group_detail_payment_meta),
                    stringResource(payment.method.labelRes),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { onEvent(GroupDetailUiEvent.OnPaymentDeleteClicked(payment.id)) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.group_detail_entry_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

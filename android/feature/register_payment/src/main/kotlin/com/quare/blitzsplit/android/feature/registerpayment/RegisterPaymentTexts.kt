package com.quare.blitzsplit.android.feature.registerpayment

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState

// Why: "Ana te pagou?", "Você pagou Bruno?" or "Diego pagou Bruno?".
@Composable
internal fun titleOf(state: RegisterPaymentUiState): String {
    val from = state.from?.name.orEmpty()
    val to = state.to?.name.orEmpty()
    return when (state.kind) {
        PaymentKind.RECEIVED -> stringResource(R.string.register_payment_title_received, from)
        PaymentKind.PAID -> stringResource(R.string.register_payment_title_paid, to)
        PaymentKind.BETWEEN_OTHERS -> stringResource(R.string.register_payment_title_between, from, to)
    }
}

// Why: "Ana te deve R$ 162,30", "Você deve R$ 118,00 a Bruno" or "Diego deve R$ 64,00 a Bruno".
@Composable
internal fun debtTextOf(
    state: RegisterPaymentUiState,
    amountMinor: Long,
    money: MoneyFormatter,
): String = pairTextOf(
    state = state,
    amountMinor = amountMinor,
    money = money,
    received = R.string.register_payment_debt_received,
    paid = R.string.register_payment_debt_paid,
    between = R.string.register_payment_debt_between,
)

// Why: under the amount, the error past the debt, or what is left of it after a partial payment.
@Composable
internal fun amountHelpOf(
    state: RegisterPaymentUiState,
    money: MoneyFormatter,
): String? {
    val remaining = state.remainingMinor
    return when {
        state.showsOverDebtError -> stringResource(
            R.string.register_payment_over_debt,
            debtTextOf(state, state.debtMinor, money),
        )

        remaining != null -> pairTextOf(
            state = state,
            amountMinor = remaining,
            money = money,
            received = R.string.register_payment_remaining_received,
            paid = R.string.register_payment_remaining_paid,
            between = R.string.register_payment_remaining_between,
        )

        else -> null
    }
}

@Composable
internal fun confirmLabelOf(kind: PaymentKind): String = stringResource(
    when (kind) {
        PaymentKind.RECEIVED -> R.string.register_payment_confirm_received
        PaymentKind.PAID -> R.string.register_payment_confirm_paid
        PaymentKind.BETWEEN_OTHERS -> R.string.register_payment_confirm_between
    },
)

// Why: the debtor is named in RECEIVED, the creditor in PAID, both in BETWEEN_OTHERS, then the amount.
@Composable
private fun pairTextOf(
    state: RegisterPaymentUiState,
    amountMinor: Long,
    money: MoneyFormatter,
    @StringRes received: Int,
    @StringRes paid: Int,
    @StringRes between: Int,
): String {
    val amount = money.toText(amountMinor)
    val from = state.from?.name.orEmpty()
    val to = state.to?.name.orEmpty()
    return when (state.kind) {
        PaymentKind.RECEIVED -> stringResource(received, from, amount)
        PaymentKind.PAID -> stringResource(paid, to, amount)
        PaymentKind.BETWEEN_OTHERS -> stringResource(between, from, to, amount)
    }
}

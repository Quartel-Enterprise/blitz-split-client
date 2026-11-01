package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency

data class RegisterPaymentUiState(
    val currency: GroupCurrency,
    // Why: null until Room is read, and for someone no longer in the group ("Ex-membro").
    val from: PaymentPerson?,
    val to: PaymentPerson?,
    // Why: "Ana te deve R$ 162,30", what the pair owes now, which a payment never goes past.
    val debtMinor: Long,
    /*
     * Why: settling up with someone across groups from the global Saldos (Acertar, Recebi): what each group adds to
     * the net, one payment each; empty for a pair of one group.
     */
    val groupParts: List<PersonGroupBalance>,
    // Why: the "Valor pago" field as typed, which opens with the whole debt.
    val amountText: String,
    // Why: once typed in, the field keeps what was typed when a sync changes the debt.
    val hasEditedAmount: Boolean,
    val method: SettlementMethod,
    // Why: "Quando", date and time; each UI writes the date, after "Hoje" or "Ontem" when it is one of them.
    val paidAt: Long,
    val paidDay: RelativeDay,
    // Why: a payment was made by now, so "Quando" picks nothing later; the time of the last change, which pickers cap at.
    val latestPaidAt: Long,
    val isOnline: Boolean,
    val isSaving: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    val kind: PaymentKind
        get() = when {
            to?.isMe == true -> PaymentKind.RECEIVED
            from?.isMe == true -> PaymentKind.PAID
            else -> PaymentKind.BETWEEN_OTHERS
        }

    val amountMinor: Long?
        get() = AmountInput.parse(amountText, currency)

    /*
     * Why: across several groups only the whole net settles, since a part of it can't say which group it closes;
     * "Toda a dívida" and the field stay as they are.
     */
    val isAmountLocked: Boolean
        get() = groupParts.size > 1

    val isAmountValid: Boolean
        get() = amountMinor?.let { if (isAmountLocked) it == debtMinor else it in 1..debtMinor } == true

    // Why: "Ana te deve R$ 162,30. Use até esse valor."
    val showsOverDebtError: Boolean
        get() = amountMinor?.let { it > debtMinor } == true

    // Why: "Ana ainda vai te dever R$ 62,30" under a partial payment.
    val remainingMinor: Long?
        get() = amountMinor?.takeIf { isAmountValid && it < debtMinor }?.let { debtMinor - it }

    // Why: "Toda a dívida" is marked while the amount is the whole debt.
    val isFullDebt: Boolean
        get() = amountMinor == debtMinor

    // Why: "Sem internet: fica salvo no aparelho e sincroniza ao reconectar."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    val canConfirm: Boolean
        get() = isLoaded && isAmountValid && !isSaving
}

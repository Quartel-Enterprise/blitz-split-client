package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

data class SplitBillUiState(
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    val currency: GroupCurrency,
    // Why: in the order they were added to the group, the creator first.
    val people: List<BillPerson>,
    // Why: the "Valor" field as typed, "312,00" or "312.00".
    val amountText: String,
    val description: String,
    // Why: a field's error only shows once it was typed in, so a new bill doesn't open in red.
    val hasEditedAmount: Boolean,
    val hasEditedDescription: Boolean,
    val payerId: String,
    // Why: the "Quem pagou" sheet, open while not null, with the person marked in it until "Pronto".
    val payerSelection: String?,
    // Why: "Quando", date and time; each UI writes the date, after "Hoje" or "Ontem" when it is one of them.
    val spentAt: Long,
    val spentDay: RelativeDay,
    // Why: a bill is spent by now, so "Quando" picks nothing later; the time of the last change, which pickers cap at.
    val latestSpentAt: Long,
    val split: SplitEditor,
    // Why: "Dividir", open while not null; it changes the bill's split only on "Pronto".
    val splitEditor: SplitEditor?,
    val isOnline: Boolean,
    val isSaving: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    val amountMinor: Long?
        get() = AmountInput.parse(amountText, currency)

    val isAmountValid: Boolean
        get() = ExpenseRules.isAmountValid(amountMinor, currency)

    // Why: "Informe quanto foi a conta.", also for an amount over the limit.
    val showsAmountError: Boolean
        get() = hasEditedAmount && !isAmountValid

    val isDescriptionValid: Boolean
        get() = ExpenseRules.isDescriptionValid(description)

    // Why: "Diga o que foi, por exemplo Mercado."
    val showsDescriptionError: Boolean
        get() = hasEditedDescription && !isDescriptionValid

    val payer: BillPerson?
        get() = people.firstOrNull { it.id == payerId }

    val payerIsMe: Boolean
        get() = payer?.isMe == true

    // Why: "Sua parte: R$ 118,00", null while the split doesn't add up or leaves this device's user out.
    val myShareMinor: Long?
        get() {
            val me = people.firstOrNull { it.isMe } ?: return null
            return split.shares?.firstOrNull { it.memberId == me.id }?.amountMinor
        }

    // Why: "Você pagou e recebe R$ 249,60 de volta."
    val refundMinor: Long?
        get() {
            val amount = amountMinor?.takeIf { isAmountValid && payerIsMe } ?: return null
            if (split.shares == null) return null
            return amount - (myShareMinor ?: 0L)
        }

    // Why: Salvar stays disabled until the bill can be saved.
    val canSave: Boolean
        get() = isLoaded && isAmountValid && isDescriptionValid && payer != null && split.addsUp && !isSaving

    // Why: "Sem internet: a conta fica salva no aparelho e entra nos saldos agora."
    val showsOfflineNotice: Boolean
        get() = !isOnline
}

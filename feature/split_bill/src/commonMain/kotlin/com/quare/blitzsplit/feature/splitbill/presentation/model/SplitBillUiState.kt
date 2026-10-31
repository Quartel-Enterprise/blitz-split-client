package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

data class SplitBillUiState(
    // Why: "Editar conta" rather than "Rachar conta", with "Apagar conta" under the form.
    val isEditing: Boolean,
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
    // Why: whoever paid the whole bill, empty while several people paid it.
    val payerId: String,
    // Why: "Várias pessoas pagaram", each one with what they paid; null while one person paid the whole bill.
    val severalPayers: PayerAmounts?,
    // Why: the "Quem pagou" sheet, open while not null, with what is marked in it until "Pronto".
    val payerSelection: PayerSelection?,
    // Why: the "Várias pessoas pagaram" sheet, open while not null; it changes who paid only on "Pronto".
    val payerAmounts: PayerAmounts?,
    // Why: "Quando", date and time; each UI writes the date, after "Hoje" or "Ontem" when it is one of them.
    val spentAt: Long,
    val spentDay: RelativeDay,
    // Why: a bill is spent by now, so "Quando" picks nothing later; the time of the last change, which pickers cap at.
    val latestSpentAt: Long,
    val split: SplitEditor,
    // Why: "Dividir", open while not null; it changes the bill's split only on "Pronto".
    val splitEditor: SplitEditor?,
    // Why: someone else saved the bill while it was being edited here.
    val conflict: BillConflict?,
    // Why: "Apagar esta conta?", open while not null.
    val deletion: BillDeletion?,
    val isOnline: Boolean,
    val isSaving: Boolean,
    val isDeleting: Boolean,
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

    // Why: whoever paid the whole bill, null while several people paid it or the payer left the group.
    val payer: BillPerson?
        get() = people.firstOrNull { it.id == payerId }

    val payerIsMe: Boolean
        get() = payer?.isMe == true

    // Why: "Ex-membro pagou": a bill being edited keeps a payer who left the group, unless someone else is picked.
    val isPayerFormerMember: Boolean
        get() = isEditing && isLoaded && payer == null && payerId.isNotEmpty()

    // Why: who paid the bill, whom the cents a split can't share evenly go to.
    val payerIds: List<String>
        get() = severalPayers?.payers?.map { it.memberId } ?: listOf(payerId).filter { it.isNotEmpty() }

    // Why: what this device's user paid of the bill, 0 when they paid nothing.
    val myPaidMinor: Long
        get() {
            val me = people.firstOrNull { it.isMe } ?: return 0L
            val several = severalPayers ?: return if (payerIsMe) amountMinor ?: 0L else 0L
            return several.payers.filter { it.memberId == me.id }.sumOf { it.amountMinor }
        }

    // Why: "Sua parte: R$ 118,00", null while the split doesn't add up or leaves this device's user out.
    val myShareMinor: Long?
        get() {
            val me = people.firstOrNull { it.isMe } ?: return null
            return split.shares?.firstOrNull { it.memberId == me.id }?.amountMinor
        }

    /*
     * Why: "Você pagou e recebe R$ 249,60 de volta.", while this device's user paid more than their part. Null while
     * they paid nothing or what they paid doesn't add up with the others yet.
     */
    val refundMinor: Long?
        get() {
            if (!isAmountValid || split.shares == null || severalPayers?.addsUp == false) return null
            val paid = myPaidMinor.takeIf { it > 0 } ?: return null
            return (paid - (myShareMinor ?: 0L)).takeIf { it >= 0 }
        }

    // Why: who paid is settled: one person (or one who left a bill being edited), or several adding up to the total.
    val isPayerValid: Boolean
        get() = severalPayers?.addsUp ?: (payer != null || isPayerFormerMember)

    // Why: Salvar stays disabled until the bill can be saved.
    val canSave: Boolean
        get() = isLoaded && isAmountValid && isDescriptionValid && isPayerValid && split.addsUp && !isSaving &&
            !isDeleting

    // Why: "Apagar conta" only when editing, and not while saving or deleting.
    val canDelete: Boolean
        get() = isEditing && isLoaded && !isSaving && !isDeleting

    // Why: "Mudar o total refaz a divisão por valor proporcionalmente.", under the amount next to its other help.
    val showsProportionalAmountHint: Boolean
        get() = isEditing && split.mode == SplitMode.AMOUNT && !showsAmountError

    // Why: "Sem internet: a conta fica salva no aparelho e entra nos saldos agora."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: "Qualquer pessoa do grupo pode editar ou apagar." under the form when editing.
    val showsAnyoneCanEditHint: Boolean
        get() = isEditing && conflict == null
}

package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.math.abs
import kotlin.math.sign

/*
 * Why: "Elisa já acertou com Bruno depois desta conta. O acerto continua valendo e o saldo é refeito: com a mudança,
 * Elisa volta a dever R$ 20,00 a Bruno." (7.4b) Editing or deleting a bill after a payment between two people whose
 * balance it moves keeps the payment and redoes the balance, which "Editar conta" and "Apagar esta conta?" say first.
 * Each UI words it: "você" for this device's user, "Ex-membro" for a null name.
 */
data class SettledPairChange(
    // Why: who paid and who received the payment, "Elisa já acertou com Bruno".
    val payerName: String?,
    val payerIsMe: Boolean,
    val receiverName: String?,
    val receiverIsMe: Boolean,
    val outcome: SettledPairOutcome,
    // Why: whoever owes the other once the change counts is the payment's payer; who paid while EVEN.
    val debtorIsPayer: Boolean,
    // Why: what the debtor owes the other once the change counts, 0 while EVEN.
    val amountMinor: Long,
    // Why: the payment's own currency, which writes amountMinor.
    val currency: GroupCurrency,
) {
    val debtorName: String?
        get() = if (debtorIsPayer) payerName else receiverName

    val debtorIsMe: Boolean
        get() = if (debtorIsPayer) payerIsMe else receiverIsMe

    val creditorName: String?
        get() = if (debtorIsPayer) receiverName else payerName

    val creditorIsMe: Boolean
        get() = if (debtorIsPayer) receiverIsMe else payerIsMe

    /*
     * Why: the pair and how it ends up, without the amount, which moves with every digit typed: a screen reader says
     * the warning again only when this changes, never on each keystroke.
     */
    val announcementKey: SettledPairChange
        get() = copy(amountMinor = 0L)

    companion object {
        /*
         * Why: the latest payment dated at or after the bill ("depois desta conta") between two people whose balance
         * the change moves, comparing the group's balances with the saved bill and with its change; a null change
         * deletes it. "After the bill" follows the Quando being typed, since once saved that is the bill's date.
         * Null when no such payment is there, so nothing is said.
         */
        fun findOrNull(
            saved: Expense,
            changed: Expense?,
            group: Group,
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): SettledPairChange? {
            /*
             * Why: a balance is per currency, so only what is in the bill's own is added up: a payment in another one
             * settles nothing of it, and a change of the currency takes the bill out of this one.
             */
            val currency = saved.currency
            val inCurrency = settlements.filter { it.currency == currency }
            val others = expenses.filter { it.id != saved.id && it.currency == currency }
            val before = GroupBalances.createUnsimplified(others + saved, inCurrency, currency)
            val after = GroupBalances.createUnsimplified(
                others + listOfNotNull(changed?.takeIf { it.currency == currency }),
                inCurrency,
                currency,
            )
            val spentAt = changed?.spentAt ?: saved.spentAt
            val settlement = inCurrency
                .filter { it.paidAt >= spentAt }
                .sortedByDescending { it.paidAt }
                .firstOrNull { before.owedOf(it) != after.owedOf(it) }
                ?: return null
            return of(
                settlement = settlement,
                owedBeforeMinor = before.owedOf(settlement),
                owedAfterMinor = after.owedOf(settlement),
                group = group,
            )
        }

        // Why: what deleting the bill would unsettle, as Room has the group now; null while the bill isn't there.
        fun findDeletionOrNull(
            expenseId: String,
            group: Group?,
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): SettledPairChange? {
            val saved = expenses.firstOrNull { it.id == expenseId } ?: return null
            return group?.let { findOrNull(saved, null, it, expenses, settlements) }
        }

        private fun of(
            settlement: Settlement,
            owedBeforeMinor: Long,
            owedAfterMinor: Long,
            group: Group,
        ): SettledPairChange {
            val payer = group.members.firstOrNull { it.id == settlement.fromMemberId }
            val receiver = group.members.firstOrNull { it.id == settlement.toMemberId }
            return SettledPairChange(
                payerName = payer?.name,
                payerIsMe = payer?.isMe == true,
                receiverName = receiver?.name,
                receiverIsMe = receiver?.isMe == true,
                outcome = outcomeOf(owedBeforeMinor, owedAfterMinor),
                debtorIsPayer = owedAfterMinor >= 0,
                amountMinor = abs(owedAfterMinor),
                currency = settlement.currency,
            )
        }

        // Why: a debt that stays the same way round but shrinks "fica devendo"; one that grows or flips "passa a dever".
        private fun outcomeOf(
            owedBeforeMinor: Long,
            owedAfterMinor: Long,
        ): SettledPairOutcome = when {
            owedAfterMinor == 0L -> SettledPairOutcome.EVEN

            owedAfterMinor > 0 && owedBeforeMinor <= 0 -> SettledPairOutcome.OWES_AGAIN

            owedAfterMinor.sign == owedBeforeMinor.sign && abs(owedAfterMinor) < abs(owedBeforeMinor) ->
                SettledPairOutcome.OWES_LESS

            else -> SettledPairOutcome.OWES
        }

        // Why: what the payment's payer owes its receiver, negative while the receiver owes the payer.
        private fun GroupBalances.owedOf(settlement: Settlement): Long =
            debtOf(settlement.fromMemberId, settlement.toMemberId) -
                debtOf(settlement.toMemberId, settlement.fromMemberId)
    }
}

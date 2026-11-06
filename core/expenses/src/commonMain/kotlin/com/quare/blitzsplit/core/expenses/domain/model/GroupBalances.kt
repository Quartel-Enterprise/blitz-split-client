package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: who owes whom in a group, from its bills' payers and shares and the payments recorded since. Each pair shows
 * once, already netted: if Ana owes you and you owe her, only the difference stays. With "Simplificar dívidas" on
 * (13.1), debts are the fewest payments that keep each one's total instead.
 */
data class GroupBalances(
    // Why: the largest first.
    val debts: List<Debt>,
    // Why: "Ninguém deve nada ainda" before the first bill; "Todo mundo acertou" once there were some.
    val hasBills: Boolean,
    // Why: the pairs as each bill made them while "Simplificar dívidas" is on; null while it is off.
    val unsimplifiedDebts: List<Debt>?,
) {
    // Why: "Dívidas simplificadas" (13.4) above the pairs, and in Saldos' "Por grupo" (13.6).
    val isSimplified: Boolean
        get() = unsimplifiedDebts != null

    // Why: "5 pagamentos em vez de 7", what the group would have with it off.
    val unsimplifiedCount: Int
        get() = (unsimplifiedDebts ?: debts).size

    // Why: "Todo mundo acertou", the card that offers "Apagar grupo".
    val isEveryoneSettled: Boolean
        get() = hasBills && debts.isEmpty()

    // Why: the icon of 13.4 on a pair no bill made, "Diego → você", which the simplification brought.
    fun isSimplifiedPair(debt: Debt): Boolean = unsimplifiedDebts?.none {
        it.fromMemberId == debt.fromMemberId && it.toMemberId == debt.toMemberId
    } == true

    /*
     * Why: whom the payer owed by the bills instead, for a pair the simplification made or grew: "Diego paga a você o
     * que devia a Bruno" (13.5), "em vez de pagar o Bruno você paga direto para mim" (13.6b). Their debts to anyone
     * else that went or shrank, by member id; null for a pair as the bills made it or one that only shrank, or while it
     * is off.
     */
    fun insteadOfOrNull(
        fromMemberId: String,
        toMemberId: String,
    ): List<String>? {
        val raw = unsimplifiedDebts ?: return null
        val debtMinor = debtOf(fromMemberId, toMemberId)
        val rawMinor = raw.firstOrNull { it.fromMemberId == fromMemberId && it.toMemberId == toMemberId }?.amountMinor
        if (debtMinor == 0L || (rawMinor != null && rawMinor >= debtMinor)) return null
        return raw
            .filter { it.fromMemberId == fromMemberId && it.toMemberId != toMemberId }
            .filter { debtOf(fromMemberId, it.toMemberId) < it.amountMinor }
            .map { it.toMemberId }
    }

    /*
     * Why: the greatest debtor pays the greatest creditor until both are even, at most members - 1 payments. Ties go
     * by the order they joined the group in (memberOrder; someone no longer in it after, by id), so every device gets
     * the same pairs, offline too. A group already at its fewest payments keeps its pairs, so turning it on never
     * moves who pays whom without saving a payment (13.3c).
     */
    fun toSimplified(memberOrder: List<String>): GroupBalances {
        val raw = unsimplifiedDebts ?: debts
        val nets = mutableMapOf<String, Long>()
        raw.forEach {
            nets[it.toMemberId] = nets.getOrElse(it.toMemberId) { 0L } + it.amountMinor
            nets[it.fromMemberId] = nets.getOrElse(it.fromMemberId) { 0L } - it.amountMinor
        }
        val rank = memberOrder.withIndex().associate { it.value to it.index }
        val largestFirst = compareByDescending<Map.Entry<String, Long>> { it.value }
            .thenBy { rank[it.key] ?: Int.MAX_VALUE }
            .thenBy { it.key }
        val credits = nets.filterValues { it > 0 }.toMutableMap()
        val owed = nets.filterValues { it < 0 }.mapValues { -it.value }.toMutableMap()
        val payments = mutableListOf<Debt>()
        while (credits.isNotEmpty() && owed.isNotEmpty()) {
            val debtor = owed.entries.minWith(largestFirst)
            val creditor = credits.entries.minWith(largestFirst)
            val paid = minOf(debtor.value, creditor.value)
            payments += Debt(debtor.key, creditor.key, paid)
            owed.reduce(debtor.key, debtor.value - paid)
            credits.reduce(creditor.key, creditor.value - paid)
        }
        return copy(
            debts = if (payments.size < raw.size) toSorted(payments) else raw,
            unsimplifiedDebts = raw,
        )
    }

    /*
     * Why: the pairs as the group shows them now, whichever switch these were worked out with, so a screen reading the
     * group and its balances from two flows never shows one's pairs with the other's switch.
     */
    fun toShownFor(group: Group): GroupBalances {
        val raw = copy(
            debts = unsimplifiedDebts ?: debts,
            unsimplifiedDebts = null,
        )
        return if (group.simplifiesDebts) raw.toSimplified(group.members.map { it.id }) else raw
    }

    private fun MutableMap<String, Long>.reduce(
        key: String,
        left: Long,
    ) {
        if (left == 0L) remove(key) else put(key, left)
    }

    /*
     * Why: "Seu saldo neste grupo" and each member's balance, also when only payments are left (their bill was
     * deleted), the same on Despesas, Saldos and Membros; null while the group never had anything to owe.
     */
    fun balanceOrNull(memberId: String?): MemberBalance? =
        memberId?.takeIf { hasBills || debts.isNotEmpty() }?.let(::balanceOf)

    fun balanceOf(memberId: String): MemberBalance = MemberBalance(
        receivesMinor = debts.filter { it.toMemberId == memberId }.sumOf { it.amountMinor },
        owesMinor = debts.filter { it.fromMemberId == memberId }.sumOf { it.amountMinor },
    )

    // Why: what Registrar pagamento opens with, and never lets a payment pass.
    fun debtOf(
        fromMemberId: String,
        toMemberId: String,
    ): Long = debts
        .firstOrNull { it.fromMemberId == fromMemberId && it.toMemberId == toMemberId }
        ?.amountMinor ?: 0L

    // Why: "Acertar" on the balance card opens Registrar pagamento with whoever the member owes the most.
    fun findLargestCreditorOrNull(memberId: String): String? = debts
        .filter { it.fromMemberId == memberId }
        .maxByOrNull { it.amountMinor }
        ?.toMemberId

    companion object {
        /*
         * Why: the pairs as the group shows them in one currency, simplified while its "Simplificar dívidas" is on.
         * Currencies are never added: a bill or a payment in another one is another balance (getByCurrency).
         */
        fun of(
            group: Group,
            expenses: List<Expense>,
            settlements: List<Settlement>,
            currency: GroupCurrency = group.currency,
        ): GroupBalances = of(
            expenses = expenses.filter { it.currency == currency },
            settlements = settlements.filter { it.currency == currency },
        ).toShownFor(group)

        /*
         * Why: one balance per currency the group has bills or payments in, its default currency first (also with
         * none, "Ninguém deve nada ainda"), then the one with the most bills, then by code.
         */
        fun getByCurrency(
            group: Group,
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): List<CurrencyBalances> = currenciesOf(group, expenses, settlements).map { currency ->
            CurrencyBalances(currency, of(group, expenses, settlements, currency))
        }

        // Why: the currencies a group's money is in, in the order getByCurrency lists them.
        fun currenciesOf(
            group: Group,
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): List<GroupCurrency> {
            val used = (expenses.map { it.currency } + settlements.map { it.currency }).distinct()
            val billCount = expenses.groupingBy { it.currency }.eachCount()
            val others = used
                .filter { it != group.currency }
                .sortedWith(compareByDescending<GroupCurrency> { billCount[it] ?: 0 }.thenBy { it.code })
            return listOf(group.currency) + others
        }

        // Why: the pairs as each bill and payment make them, whatever the group's "Simplificar dívidas".
        fun of(
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): GroupBalances {
            val owed = mutableMapOf<Pair<String, String>, Long>()
            expenses.flatMap(::findDebtsWithin).forEach { owed.add(it.fromMemberId, it.toMemberId, it.amountMinor) }
            // Why: a payment from Ana to Bruno cancels that much of what Ana owes him, or makes him owe her past it.
            settlements.forEach { owed.add(it.toMemberId, it.fromMemberId, it.amountMinor) }
            val debts = owed.keys
                .map { (from, to) -> if (from < to) from to to else to to from }
                .distinct()
                .mapNotNull { (first, second) ->
                    val net = owed.getOrElse(first to second) { 0L } - owed.getOrElse(second to first) { 0L }
                    when {
                        net > 0 -> Debt(first, second, net)
                        net < 0 -> Debt(second, first, -net)
                        else -> null
                    }
                }
            return GroupBalances(
                debts = toSorted(debts),
                hasBills = expenses.isNotEmpty(),
                unsimplifiedDebts = null,
            )
        }

        private fun toSorted(debts: List<Debt>): List<Debt> = debts.sortedWith(
            compareByDescending<Debt> { it.amountMinor }
                .thenBy { it.fromMemberId }
                .thenBy { it.toMemberId },
        )

        /*
         * Why: within one bill, whoever paid more than their part is owed by whoever paid less, matched in the
         * bill's order. With one payer, everyone else owes that payer their part; with several, the amounts still
         * add up to the cent. Conta's "Sua parte" names the same people Saldos counts the bill toward.
         */
        fun findDebtsWithin(expense: Expense): List<Debt> {
            val memberIds = (expense.shares.map { it.memberId } + expense.payers.map { it.memberId }).distinct()
            val nets = memberIds.associateWith { expense.balanceOf(it) ?: 0L }
            val creditors = memberIds.filter { nets.getValue(it) > 0 }.map { it to nets.getValue(it) }.toMutableList()
            val debtors = memberIds.filter { nets.getValue(it) < 0 }.map { it to -nets.getValue(it) }
            val debts = mutableListOf<Debt>()
            var creditorIndex = 0
            debtors.forEach { (debtorId, owes) ->
                var left = owes
                while (left > 0 && creditorIndex < creditors.size) {
                    val (creditorId, credit) = creditors[creditorIndex]
                    val paid = minOf(left, credit)
                    debts += Debt(debtorId, creditorId, paid)
                    left -= paid
                    creditors[creditorIndex] = creditorId to credit - paid
                    if (credit == paid) creditorIndex++
                }
            }
            return debts
        }

        private fun MutableMap<Pair<String, String>, Long>.add(
            fromMemberId: String,
            toMemberId: String,
            amountMinor: Long,
        ) {
            val key = fromMemberId to toMemberId
            this[key] = getOrElse(key) { 0L } + amountMinor
        }
    }
}

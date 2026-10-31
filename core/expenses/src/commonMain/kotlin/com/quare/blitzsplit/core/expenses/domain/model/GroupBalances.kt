package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: who owes whom in a group, from its bills' payers and shares and the payments recorded since. Each pair shows
 * once, already netted: if Ana owes you and you owe her, only the difference stays.
 */
data class GroupBalances(
    // Why: the largest first.
    val debts: List<Debt>,
    // Why: "Ninguém deve nada ainda" before the first bill; "Todo mundo acertou" once there were some.
    val hasBills: Boolean,
) {
    // Why: "Todo mundo acertou", the card that offers "Apagar grupo".
    val isEveryoneSettled: Boolean
        get() = hasBills && debts.isEmpty()

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
        fun of(
            expenses: List<Expense>,
            settlements: List<Settlement>,
        ): GroupBalances {
            val owed = mutableMapOf<Pair<String, String>, Long>()
            expenses.forEach { expense -> owed.addDebtsOf(expense) }
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
                }.sortedWith(
                    compareByDescending<Debt> { it.amountMinor }
                        .thenBy { it.fromMemberId }
                        .thenBy { it.toMemberId },
                )
            return GroupBalances(
                debts = debts,
                hasBills = expenses.isNotEmpty(),
            )
        }

        /*
         * Why: within one bill, whoever paid more than their part is owed by whoever paid less, matched in the
         * bill's order. With one payer, everyone else owes that payer their part; with several, the amounts still
         * add up to the cent.
         */
        private fun MutableMap<Pair<String, String>, Long>.addDebtsOf(expense: Expense) {
            val memberIds = (expense.shares.map { it.memberId } + expense.payers.map { it.memberId }).distinct()
            val nets = memberIds.associateWith { expense.balanceOf(it) ?: 0L }
            val creditors = memberIds.filter { nets.getValue(it) > 0 }.map { it to nets.getValue(it) }.toMutableList()
            val debtors = memberIds.filter { nets.getValue(it) < 0 }.map { it to -nets.getValue(it) }.toMutableList()
            var creditorIndex = 0
            debtors.forEach { (debtorId, owes) ->
                var left = owes
                while (left > 0 && creditorIndex < creditors.size) {
                    val (creditorId, credit) = creditors[creditorIndex]
                    val paid = minOf(left, credit)
                    add(debtorId, creditorId, paid)
                    left -= paid
                    creditors[creditorIndex] = creditorId to credit - paid
                    if (credit == paid) creditorIndex++
                }
            }
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

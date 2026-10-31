package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: the rules of "Rachar conta", the same the API's check keeps.
object ExpenseRules {
    const val DESCRIPTION_MAX_LENGTH = 100

    // Why: "até R$ 100 milhões", in the major unit of whatever currency the group splits in.
    private const val MAX_AMOUNT_MAJOR = 100_000_000L

    fun maxAmountMinorFor(currency: GroupCurrency): Long = MAX_AMOUNT_MAJOR * minorPerMajorFor(currency)

    fun isAmountValid(
        amountMinor: Long?,
        currency: GroupCurrency,
    ): Boolean = amountMinor != null && amountMinor in 1..maxAmountMinorFor(currency)

    fun isDescriptionValid(description: String): Boolean = description.isNotBlank()

    /*
     * Why: what the API's expense_adds_up check keeps, so a bill it would refuse never waits in the sync queue for
     * good: at least one payer and one share, each member once in each, no negative amount, both adding up to the
     * total, and the percentages adding up to 100 for "Porcentagem".
     */
    fun isSplitValid(
        amountMinor: Long,
        splitMode: SplitMode,
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
    ): Boolean = payers.isNotEmpty() &&
        shares.isNotEmpty() &&
        payers.distinctBy { it.memberId }.size == payers.size &&
        shares.distinctBy { it.memberId }.size == shares.size &&
        payers.all { it.amountMinor >= 0 } &&
        shares.all { it.amountMinor >= 0 && it.weight >= 0 } &&
        payers.sumOf { it.amountMinor } == amountMinor &&
        shares.sumOf { it.amountMinor } == amountMinor &&
        (splitMode != SplitMode.PERCENT || shares.sumOf { it.weight } == BillSplit.FULL_PERCENT)

    // Why: never ends on half of an emoji, which would leave a lone surrogate on the way to JSON and Postgres.
    fun toCapped(description: String): String {
        val capped = description.take(DESCRIPTION_MAX_LENGTH)
        return if (capped.lastOrNull()?.isHighSurrogate() == true) capped.dropLast(1) else capped
    }

    fun minorPerMajorFor(currency: GroupCurrency): Long {
        var factor = 1L
        repeat(currency.fractionDigits) { factor *= 10 }
        return factor
    }
}

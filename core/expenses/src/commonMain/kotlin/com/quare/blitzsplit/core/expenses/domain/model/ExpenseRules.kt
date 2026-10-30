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

    fun toCapped(description: String): String = description.take(DESCRIPTION_MAX_LENGTH)

    fun minorPerMajorFor(currency: GroupCurrency): Long {
        var factor = 1L
        repeat(currency.fractionDigits) { factor *= 10 }
        return factor
    }
}

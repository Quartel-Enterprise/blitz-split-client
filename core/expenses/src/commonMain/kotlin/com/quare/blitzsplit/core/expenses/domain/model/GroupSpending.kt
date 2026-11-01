package com.quare.blitzsplit.core.expenses.domain.model

import kotlinx.datetime.TimeZone

/*
 * Why: what a group's statistics count over a period: only its bills, never the payments recorded in Saldos, those
 * still in the queue included. Consumo is each member's part as each bill split it; Pagou what each one paid then.
 */
data class GroupSpending(
    // Why: the bills spent within the period, the most recent first.
    val expenses: List<Expense>,
    val totalMinor: Long,
    val pendingCount: Int,
    val consumedByMember: Map<String, Long>,
    val paidByMember: Map<String, Long>,
    // Why: "Maior despesa", the most recent one when two cost the same; null without bills.
    val biggest: Expense?,
) {
    // Why: "Você pagou … · 3 despesas", the bills this member paid all or part of.
    fun countPaidBy(memberId: String): Int =
        expenses.count { expense -> expense.payers.any { it.memberId == memberId } }

    companion object {
        fun of(
            expenses: List<Expense>,
            span: DateSpan,
            timeZone: TimeZone,
        ): GroupSpending {
            val within = expenses
                .filter { SpentDay.dateOf(it.spentAt, timeZone) in span }
                .sortedByDescending { it.spentAt }
            return GroupSpending(
                expenses = within,
                totalMinor = within.sumOf { it.amountMinor },
                pendingCount = within.count { it.isPending },
                consumedByMember = toSumByMember(
                    within.flatMap { expense ->
                        expense.shares.map {
                            it.memberId to
                                it.amountMinor
                        }
                    },
                ),
                paidByMember = toSumByMember(
                    within.flatMap { expense ->
                        expense.payers.map {
                            it.memberId to
                                it.amountMinor
                        }
                    },
                ),
                biggest = within.maxByOrNull { it.amountMinor },
            )
        }

        private fun toSumByMember(parts: List<Pair<String, Long>>): Map<String, Long> =
            parts.groupBy({ it.first }, { it.second }).mapValues { (_, amounts) -> amounts.sum() }
    }
}

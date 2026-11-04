package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsSettler
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsSettling
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil

/*
 * Why: "Acertos" (Pierre's call, beta feedback 12): how long each member takes to cover what the period's bills put
 * on them. Each member's bills and payments, in the group's whole history, go in date order: what a bill puts on
 * them opens a debt on its "Quando", and what they pay (a payment, or a later bill they paid more of than their
 * part) closes the oldest debts first. A debt still open counts up to today. Only debts born of the period's bills
 * count, whenever they were covered; ex-members are left out, guests stay, since anyone records their payments.
 */
internal class SettlingCalculator(
    private val group: Group,
    private val expenses: List<Expense>,
    private val settlements: List<Settlement>,
    private val today: LocalDate,
    private val timeZone: TimeZone,
) {
    /*
     * Why: null until two people had a debt in the period, so there is someone to compare, and while the fastest and
     * the slowest tie (Pierre's call): nobody is slower than anyone then.
     */
    fun createOrNull(span: DateSpan): StatsSettling? {
        val settlers = group.members
            .mapNotNull { member -> createSettlerOrNull(member, span) }
            .sortedWith(compareBy<StatsSettler> { it.averageDays }.thenBy { it.openMinor })
        if (settlers.size < MIN_SETTLERS) return null
        val fastest = settlers.first()
        val slowest = settlers.last()
        if (fastest.averageDays == slowest.averageDays && fastest.openMinor == slowest.openMinor) return null
        return StatsSettling(
            fastest = fastest,
            slowest = slowest,
        )
    }

    private fun createSettlerOrNull(
        member: GroupMember,
        span: DateSpan,
    ): StatsSettler? {
        val debts = ArrayDeque<Debt>()
        var credit = 0L
        var weightedDays = 0L
        var countedMinor = 0L
        findMovesOf(member.id).forEach { move ->
            val day = SpentDay.dateOf(move.at, timeZone)
            if (move.amountMinor > 0) {
                var left = move.amountMinor
                while (left > 0 && debts.isNotEmpty()) {
                    val debt = debts.first()
                    val covered = minOf(left, debt.amountMinor)
                    if (debt.counts) {
                        weightedDays += covered * debt.openedOn.daysUntil(day).coerceAtLeast(0)
                        countedMinor += covered
                    }
                    debt.amountMinor -= covered
                    left -= covered
                    if (debt.amountMinor == 0L) debts.removeFirst()
                }
                credit += left
            } else {
                // Why: what the others already owed them covers a new part at once, so it is no debt.
                val owed = -move.amountMinor
                val fromCredit = minOf(credit, owed)
                credit -= fromCredit
                if (owed > fromCredit) {
                    debts.addLast(Debt(day, owed - fromCredit, counts = move.isBill && day in span))
                }
            }
        }
        val open = debts.filter { it.counts }
        open.forEach { debt ->
            weightedDays += debt.amountMinor * debt.openedOn.daysUntil(today).coerceAtLeast(0)
            countedMinor += debt.amountMinor
        }
        if (countedMinor == 0L) return null
        return StatsSettler(
            memberId = member.id,
            name = member.name,
            initials = Initials.ofMember(member.name, member.isMe),
            avatarColor = member.avatarColor,
            photoUrl = member.photoUrl,
            isMe = member.isMe,
            averageDays = ((weightedDays + countedMinor / 2) / countedMinor).toInt(),
            openMinor = open.sumOf { it.amountMinor },
        )
    }

    // Why: in date order, a bill before a payment of the same instant; a payment received owes that much back.
    private fun findMovesOf(memberId: String): List<Move> {
        val bills = expenses.mapNotNull { expense ->
            expense.balanceOf(memberId)?.takeIf { it != 0L }?.let { Move(expense.spentAt, it, isBill = true) }
        }
        val payments = settlements.mapNotNull { payment ->
            when (memberId) {
                payment.fromMemberId -> Move(payment.paidAt, payment.amountMinor, isBill = false)
                payment.toMemberId -> Move(payment.paidAt, -payment.amountMinor, isBill = false)
                else -> null
            }
        }
        return (bills + payments).sortedWith(compareBy<Move> { it.at }.thenBy { !it.isBill })
    }

    // Why: what a bill or a payment changes in the member's balance, positive when it is in their favor.
    private class Move(
        val at: Long,
        val amountMinor: Long,
        val isBill: Boolean,
    )

    private class Debt(
        val openedOn: LocalDate,
        var amountMinor: Long,
        val counts: Boolean,
    )

    private companion object {
        const val MIN_SETTLERS = 2
    }
}

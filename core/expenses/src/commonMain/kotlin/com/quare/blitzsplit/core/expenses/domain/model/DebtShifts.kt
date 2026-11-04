package com.quare.blitzsplit.core.expenses.domain.model

import kotlin.math.abs

// Why: what moves from one set of a group's pairs to another, when "Simplificar dívidas" is turned on or off.
object DebtShifts {
    /*
     * Why: "O que muda" (13.3), only whoever something changes for, this device's user first (meId), then the order
     * they joined in (memberOrder), someone no longer in the group last.
     */
    fun shiftsOf(
        before: List<Debt>,
        after: List<Debt>,
        memberOrder: List<String>,
        meId: String?,
    ): List<DebtShift> {
        val rank = memberOrder.withIndex().associate { it.value to it.index }
        val memberIds = (before + after).flatMap { listOf(it.fromMemberId, it.toMemberId) }.distinct()
        return memberIds
            .mapNotNull { memberId -> shiftOrNull(memberId, before, after) }
            .sortedWith(
                compareByDescending<DebtShift> { it.memberId == meId }
                    .thenBy { rank[it.memberId] ?: Int.MAX_VALUE }
                    .thenBy { it.memberId },
            )
    }

    /*
     * Why: "Ver mudanças" of Atividade (13.7b): each person whose balance with memberId moved, the largest change first,
     * so "Agora Diego te paga R$ 64,00" names the pair that moved the most; on a tie a new pair before one that went,
     * as the sentence names them.
     */
    fun counterpartShiftsOf(
        before: List<Debt>,
        after: List<Debt>,
        memberId: String,
    ): List<CounterpartShift> {
        val was = netsOf(before, memberId)
        val now = netsOf(after, memberId)
        return (was.keys + now.keys)
            .map { CounterpartShift(it, was[it] ?: 0L, now[it] ?: 0L) }
            .filter { it.beforeMinor != it.afterMinor }
            .sortedWith(
                compareByDescending<CounterpartShift> { abs(it.afterMinor - it.beforeMinor) }
                    .thenByDescending { it.beforeMinor == 0L }
                    .thenBy { it.memberId },
            )
    }

    private fun netsOf(
        debts: List<Debt>,
        memberId: String,
    ): Map<String, Long> = debts
        .mapNotNull {
            when (memberId) {
                it.toMemberId -> it.fromMemberId to it.amountMinor
                it.fromMemberId -> it.toMemberId to -it.amountMinor
                else -> null
            }
        }.toMap()

    private fun shiftOrNull(
        memberId: String,
        before: List<Debt>,
        after: List<Debt>,
    ): DebtShift? {
        val paidBefore = before.filter { it.fromMemberId == memberId }.associateBy { it.toMemberId }
        val paysAfter = after.filter { it.fromMemberId == memberId }
        val receivedBefore = before.filter { it.toMemberId == memberId }.map { it.fromMemberId }.toSet()
        val receivesAfter = after.filter { it.toMemberId == memberId }.map { it.fromMemberId }
        val paysChanged = paysAfter.any { paidBefore[it.toMemberId]?.amountMinor != it.amountMinor }
        val shift = DebtShift(
            memberId = memberId,
            pays = if (paysChanged) paysAfter else emptyList(),
            stopsPaying = paidBefore.values.filter { old -> paysAfter.none { it.toMemberId == old.toMemberId } },
            receivesFrom = if (receivesAfter.any { it !in receivedBefore }) receivesAfter else emptyList(),
            stopsReceivingFrom = receivedBefore.filter { it !in receivesAfter },
        )
        return shift.takeIf {
            it.pays.isNotEmpty() ||
                it.stopsPaying.isNotEmpty() ||
                it.receivesFrom.isNotEmpty() ||
                it.stopsReceivingFrom.isNotEmpty()
        }
    }
}

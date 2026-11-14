package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/*
 * Why: what "Export group data" writes, read once from the device, so it works offline: the members, the bills and the
 * payments oldest first, and each member's balance in each currency. Every format is drawn from this, so a new field
 * (a bill's payment method, say) is one property here and one column in GroupExportTables and the JSON.
 */
data class GroupExport(
    val groupName: String,
    // Why: the day it was made, on the device's calendar, which the file is named after.
    val exportedOn: LocalDate,
    // Why: the group's members in the group's order, then whoever left it and still is in a bill or a payment.
    val members: List<ExportMember>,
    val bills: List<ExportBill>,
    val payments: List<ExportPayment>,
    val balances: List<ExportCurrencyBalances>,
) {
    companion object {
        fun of(
            group: Group,
            expenses: List<Expense>,
            settlements: List<Settlement>,
            categoryTree: CategoryTree,
            timeZone: TimeZone,
            nowMillis: Long,
        ): GroupExport {
            val current = group.members.map {
                ExportMember(
                    id = it.id,
                    name = it.name,
                    isMe = it.isMe,
                    hasJoined = it.hasJoined,
                )
            }
            val currentIds = current.map { it.id }.toSet()
            val sortedExpenses = expenses.sortedBy { it.spentAt }
            val sortedSettlements = settlements.sortedBy { it.paidAt }
            // Why: numbered in the order they first show up, the oldest bill first, then the payments.
            val formerIds = (
                sortedExpenses.flatMap { expense ->
                    expense.payers.map { it.memberId } + expense.shares.map { it.memberId }
                } +
                    sortedSettlements.flatMap { listOf(it.fromMemberId, it.toMemberId) }
            ).distinct().filterNot { it in currentIds }
            val members =
                current + formerIds.map {
                    ExportMember(
                        id = it,
                        name = null,
                        isMe = false,
                        hasJoined = false,
                    )
                }
            val bills = sortedExpenses.map { expense ->
                ExportBill(
                    id = expense.id,
                    date = SpentDay.dateOf(expense.spentAt, timeZone),
                    spentAt = expense.spentAt,
                    description = expense.description,
                    category = categoryTree.resolve(expense.categoryId),
                    amountMinor = expense.amountMinor,
                    currency = expense.currency,
                    payers = expense.payers.map { ExportAmount(it.memberId, it.amountMinor) },
                    shares = expense.shares.map { ExportAmount(it.memberId, it.amountMinor) },
                )
            }
            val payments = sortedSettlements.map {
                ExportPayment(
                    id = it.id,
                    date = SpentDay.dateOf(it.paidAt, timeZone),
                    paidAt = it.paidAt,
                    fromMemberId = it.fromMemberId,
                    toMemberId = it.toMemberId,
                    amountMinor = it.amountMinor,
                    currency = it.currency,
                    method = it.method,
                )
            }
            val balances = GroupBalances.getByCurrency(group, expenses, settlements).map { (currency, pairs) ->
                ExportCurrencyBalances(
                    currency = currency,
                    // Why: every member of the group, settled ones at zero, and whoever left only while not settled.
                    members = members.mapNotNull { member ->
                        val net = pairs.balanceOf(member.id).netMinor
                        if (member.id !in currentIds && net == 0L) null else ExportAmount(member.id, net)
                    },
                )
            }
            return GroupExport(
                groupName = group.name,
                exportedOn = SpentDay.dateOf(nowMillis, timeZone),
                members = members,
                bills = bills,
                payments = payments,
                balances = balances,
            )
        }
    }
}

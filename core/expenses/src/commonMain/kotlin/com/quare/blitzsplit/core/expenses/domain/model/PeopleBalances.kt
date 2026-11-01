package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.math.abs

// Why: the global Saldos, everyone this device's user has an open balance with, worked out from the device's data.
data class PeopleBalances(
    // Why: the largest net first, whichever its direction.
    val people: List<PersonBalance>,
    // Why: one per currency of the groups, the currency with the most groups first, as the home's "Saldo geral".
    val totals: List<PeopleTotal>,
    // Why: this device's user as the most recently active group has them, the payer or receiver of "Acertar".
    val me: GroupMember?,
) {
    fun findPersonOrNull(
        key: String,
        currency: String,
    ): PersonBalance? = people.firstOrNull { it.key == key && it.currency.code == currency }

    companion object {
        /*
         * Why: groups in the order of their last activity, so a person is named as in the group they were last
         * active in. Each group adds the pair between this device's user and each member, as its own Saldos nets it.
         */
        fun of(
            groups: List<Group>,
            money: Map<String, GroupMoney>,
        ): PeopleBalances {
            val parts = groups.flatMap { group ->
                val ofGroup = money[group.id] ?: GroupMoney.NONE
                partsOf(group, ofGroup)
            }
            val people = parts
                .groupBy { it.key to it.group.currency }
                .map { (keyAndCurrency, ofPerson) -> personOf(keyAndCurrency.first, ofPerson, money) }
                // Why: someone whose groups offset each other to zero owes nothing and is owed nothing.
                .filter { it.netMinor != 0L }
                .withIndex()
                .sortedWith(
                    compareByDescending<IndexedValue<PersonBalance>> { abs(it.value.netMinor) }.thenBy { it.index },
                ).map { it.value }
            return PeopleBalances(
                people = people,
                totals = totalsOf(groups, people),
                me = groups.firstNotNullOfOrNull { group -> group.members.firstOrNull { it.isMe } },
            )
        }

        /*
         * Why: someone who joined is their account in every group. A name nobody joined as is the same person in
         * every group where it is written the same, as a group of friends tracked by one person has them; settling
         * still pays each group its own pair, so two people of one name never move money between groups. Someone no
         * longer in the group is only themselves.
         */
        fun keyOf(member: GroupMember): String = member.userId?.let { "user:$it" }
            ?: "name:${member.name.lowercase().split(' ', '\t', '\n').filter { it.isNotEmpty() }.joinToString(" ")}"

        private fun goneKeyOf(
            groupId: String,
            memberId: String,
        ): String = "gone:$groupId:$memberId"

        private fun partsOf(
            group: Group,
            money: GroupMoney,
        ): List<Part> {
            val me = group.members.firstOrNull { it.isMe } ?: return emptyList()
            val balances = GroupBalances.of(money.expenses, money.settlements)
            return balances.debts
                .filter { it.fromMemberId == me.id || it.toMemberId == me.id }
                .map { debt ->
                    val otherId = if (debt.fromMemberId == me.id) debt.toMemberId else debt.fromMemberId
                    val other = group.members.firstOrNull { it.id == otherId }
                    Part(
                        key = other?.let(::keyOf) ?: goneKeyOf(group.id, otherId),
                        group = group,
                        member = other,
                        balance = PersonGroupBalance(
                            groupId = group.id,
                            groupName = group.name,
                            groupInitials = Initials.of(group.name),
                            groupColor = group.color,
                            memberId = otherId,
                            meMemberId = me.id,
                            amountMinor = if (debt.toMemberId == me.id) debt.amountMinor else -debt.amountMinor,
                        ),
                    )
                }
        }

        private fun personOf(
            key: String,
            parts: List<Part>,
            money: Map<String, GroupMoney>,
        ): PersonBalance {
            val named = parts.firstOrNull { it.member != null }?.member
            val netMinor = parts.sumOf { it.balance.amountMinor }
            val isGuest = named != null && parts.none { it.member?.hasJoined == true }
            val guestBills = if (isGuest) guestExpensesOf(parts, money) else emptyList()
            return PersonBalance(
                key = key,
                name = named?.name,
                face = named?.let(::faceOf),
                currency = parts.first().group.currency,
                netMinor = netMinor,
                groups = parts.map { it.balance },
                isGuest = isGuest,
                guestBills = guestBills.map { it.second.description }.distinct(),
                guestPaidMinor = if (isGuest) guestPaidOf(guestBills, netMinor) else null,
            )
        }

        // Why: the bills a guest has a part in, with their member in that bill's group.
        private fun guestExpensesOf(
            parts: List<Part>,
            money: Map<String, GroupMoney>,
        ): List<Pair<String, Expense>> = parts.flatMap { part ->
            money[part.group.id]
                ?.expenses
                .orEmpty()
                .filter { expense -> expense.shares.any { it.memberId == part.balance.memberId } }
                .map { part.balance.memberId to it }
        }

        private fun guestPaidOf(
            bills: List<Pair<String, Expense>>,
            netMinor: Long,
        ): Long? {
            val shareMinor = bills.sumOf { (memberId, expense) ->
                expense.shares.filter { it.memberId == memberId }.sumOf { it.amountMinor }
            }
            val paidMinor = bills.sumOf { (memberId, expense) ->
                expense.payers.filter { it.memberId == memberId }.sumOf { it.amountMinor }
            }
            return paidMinor.takeIf { it > 0 && shareMinor - it == netMinor }
        }

        private fun totalsOf(
            groups: List<Group>,
            people: List<PersonBalance>,
        ): List<PeopleTotal> = groups
            .groupBy { it.currency }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<GroupCurrency, List<Group>>> { it.value.size }.thenBy { it.key.code },
            ).map { (currency, _) ->
                val ofCurrency = people.filter { it.currency == currency }
                val receiving = ofCurrency.filter { it.netMinor > 0 }
                val paying = ofCurrency.filter { it.netMinor < 0 }
                PeopleTotal(
                    currency = currency,
                    netMinor = ofCurrency.sumOf { it.netMinor },
                    toReceiveMinor = receiving.sumOf { it.netMinor },
                    toPayMinor = -paying.sumOf { it.netMinor },
                    toReceiveCount = receiving.size,
                    toPayCount = paying.size,
                )
            }
    }

    private class Part(
        val key: String,
        val group: Group,
        val member: GroupMember?,
        val balance: PersonGroupBalance,
    )
}

package com.quare.blitzsplit.core.expenses.domain.model

// Why: the bills that give a member a part, and those parts summed, in the group's currency.
data class MemberBillParts(
    val billCount: Int,
    val shareMinor: Long,
) {
    companion object {
        // Why: a bill at 0% for the member puts nothing in their name, so it doesn't count.
        fun of(
            expenses: List<Expense>,
            memberId: String,
        ): MemberBillParts {
            val parts = expenses.map { it.shareOf(memberId) }.filter { it > 0L }
            return MemberBillParts(
                billCount = parts.size,
                shareMinor = parts.sum(),
            )
        }
    }
}

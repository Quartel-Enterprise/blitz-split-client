package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.feature.groups.presentation.model.LatestBill
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBillAuthor

/*
 * Why: the bill saved last, by when it was saved, not its "Quando". A change still in the queue was made on this
 * device, so its user saved it. A synced bill whose editor is no member of the group anymore (they left, their name was
 * released, or the server recorded no one) has updatedByMemberId null too, and names no one.
 */
internal fun latestBillOf(groups: List<GroupWithMoney>): LatestBill? {
    val item = groups
        .filter { it.expenses.isNotEmpty() }
        .maxByOrNull { item -> item.expenses.maxOf { it.updatedAt } }
        ?: return null
    val expense = item.expenses.maxBy { it.updatedAt }
    val author = expense.updatedByMemberId?.let { id -> item.group.members.firstOrNull { it.id == id } }
    return LatestBill(
        description = expense.description,
        amountMinor = expense.amountMinor,
        currency = item.group.currency,
        groupName = item.group.name,
        savedBy = when {
            expense.isPending || author?.isMe == true -> LatestBillAuthor.Me
            author != null -> LatestBillAuthor.Member(author.name)
            else -> LatestBillAuthor.Unknown
        },
    )
}

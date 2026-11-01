package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.feature.groups.presentation.model.LatestBill
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBillAuthor

/*
 * Why: the bill saved last, by when it was saved, not its "Quando". A bill no server stamped (updatedByMemberId null)
 * was only changed on this device, so its user saved it.
 */
internal fun latestBillOf(groups: List<GroupWithMoney>): LatestBill? {
    val (item, expense) = groups
        .flatMap { item -> item.expenses.map { item to it } }
        .maxByOrNull { (_, expense) -> expense.updatedAt }
        ?: return null
    val author = expense.updatedByMemberId?.let { id -> item.group.members.firstOrNull { it.id == id } }
    return LatestBill(
        description = expense.description,
        amountMinor = expense.amountMinor,
        currency = item.group.currency,
        groupName = item.group.name,
        savedBy = when {
            expense.updatedByMemberId == null || author?.isMe == true -> LatestBillAuthor.Me
            author != null -> LatestBillAuthor.Member(author.name)
            else -> LatestBillAuthor.Unknown
        },
    )
}

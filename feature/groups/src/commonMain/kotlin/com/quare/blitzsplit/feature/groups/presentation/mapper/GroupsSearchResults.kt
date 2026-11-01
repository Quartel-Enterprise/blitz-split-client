package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.feature.groups.presentation.model.GroupNameFilter
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchPersonHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch

/*
 * Why: what "Buscar" finds among what Room holds on this device, folded here rather than with a LIKE, since SQLite
 * folds neither accents nor every case; the device holds a few hundred groups and bills at most. In "Tudo" a settled
 * group's bill comes as its group ("Quitado · despesa “Churros”"), as the design draws it; "Despesas" lists it too.
 */
internal fun searchResultsOf(
    groups: List<GroupWithMoney>,
    rows: List<GroupRow>,
    query: String,
    scope: SearchScope,
): GroupsSearchResults {
    val folded = GroupNameFilter.toFolded(query.trim())
    if (folded.isEmpty()) return GroupsSearchResults.none
    val byId = groups.associateBy { it.group.id }
    // Why: the home's order, the most recent activity first, which the rows already have.
    val ordered = rows.mapNotNull { row -> byId[row.id]?.let { row to it } }
    // Why: in "Tudo" the settled groups' bills already came as their groups.
    val billGroups = ordered
        .filter { (row, _) -> scope == SearchScope.BILLS || !row.isSettled }
        .map { (_, item) -> item }
    return GroupsSearchResults(
        groups = if (scope.isLookingAt(SearchScope.GROUPS)) groupHitsOf(ordered, folded) else emptyList(),
        people = if (scope.isLookingAt(SearchScope.PEOPLE)) personHitsOf(ordered, folded) else emptyList(),
        bills = if (scope.isLookingAt(SearchScope.BILLS)) billHitsOf(billGroups, folded) else emptyList(),
    )
}

private fun SearchScope.isLookingAt(section: SearchScope): Boolean = this == SearchScope.ALL || this == section

private fun groupHitsOf(
    groups: List<Pair<GroupRow, GroupWithMoney>>,
    foldedQuery: String,
): List<SearchGroupHit> = groups.mapNotNull { (row, item) ->
    val nameMatch = GroupNameFilter.findMatchOrNull(row.name, foldedQuery)
    when {
        nameMatch != null -> SearchGroupHit(
            group = row,
            nameMatch = nameMatch,
            billDescription = null,
            billMatch = null,
        )

        row.isSettled ->
            item.expenses
                .sortedByDescending { it.spentAt }
                .firstNotNullOfOrNull { expense ->
                    GroupNameFilter.findMatchOrNull(expense.description, foldedQuery)?.let { match ->
                        SearchGroupHit(
                            group = row,
                            nameMatch = null,
                            billDescription = expense.description,
                            billMatch = match,
                        )
                    }
                }

        else -> null
    }
}

private fun personHitsOf(
    groups: List<Pair<GroupRow, GroupWithMoney>>,
    foldedQuery: String,
): List<SearchPersonHit> = groups.flatMap { (_, item) ->
    val group = item.group
    group.members
        .filter { !it.isMe }
        .mapNotNull { member ->
            GroupNameFilter.findMatchOrNull(member.name, foldedQuery)?.let { match ->
                SearchPersonHit(
                    memberId = member.id,
                    name = member.name,
                    nameMatch = match,
                    initial = Initials.firstOf(member.name),
                    color = member.avatarColor,
                    groupId = group.id,
                    groupName = group.name,
                )
            }
        }
}

private fun billHitsOf(
    groups: List<GroupWithMoney>,
    foldedQuery: String,
): List<SearchBillHit> = groups
    .flatMap { item ->
        val payersOf = PayersOfGroup(item.group)
        item.expenses.mapNotNull { expense ->
            GroupNameFilter.findMatchOrNull(expense.description, foldedQuery)?.let { match ->
                billHitOf(item.group, expense, match, payersOf)
            }
        }
    }.sortedByDescending { it.spentAt }

// Why: the group's names and payer order, built once per group rather than once per bill found.
private class PayersOfGroup(
    group: Group,
) {
    private val namesById = group.members.associate { it.id to it.name }
    private val meId = group.members.firstOrNull { it.isMe }?.id
    private val order = PayerOrder(group.members)

    fun of(expense: Expense): List<SearchBillPayer> = order.sort(expense.payers.map { it.memberId }).map { id ->
        SearchBillPayer(
            name = namesById[id],
            isMe = id == meId,
        )
    }
}

private fun billHitOf(
    group: Group,
    expense: Expense,
    match: TextMatch,
    payersOf: PayersOfGroup,
): SearchBillHit = SearchBillHit(
    id = expense.id,
    groupId = group.id,
    groupName = group.name,
    description = expense.description,
    descriptionMatch = match,
    totalMinor = expense.amountMinor,
    currency = group.currency,
    payers = payersOf.of(expense),
    spentAt = expense.spentAt,
)

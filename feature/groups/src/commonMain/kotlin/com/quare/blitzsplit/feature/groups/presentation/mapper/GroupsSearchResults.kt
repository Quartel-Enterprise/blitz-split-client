package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.utils.TypoTolerantQuery
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchContactHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchPersonHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch

// Why: the search with what its text and scope find, wherever it opens: Grupos' place or Route.Search.
internal fun GroupsSearch.findResultsIn(
    groups: List<GroupWithMoney>,
    rows: List<GroupRow>,
): GroupsSearch = copy(
    results = searchResultsOf(
        groups = groups,
        rows = rows,
        query = query,
        scope = scope,
    ),
)

/*
 * Why: what "Buscar" finds among what Room holds on this device, folded here rather than with a LIKE, since SQLite
 * folds neither accents nor every case, and typo tolerant (TypoTolerantQuery); the device holds a few hundred groups
 * and bills at most. In "Tudo" a settled group's bill comes as its group ("Quitado · despesa “Churros”"), as the design
 * draws it; "Despesas" lists it too. Only when nothing holds the text typed, what comes close a typo or two away, each
 * section the closest first, then in its own order (Pierre's beta feedback: "Larisa" finds "Larissa").
 */
internal fun searchResultsOf(
    groups: List<GroupWithMoney>,
    rows: List<GroupRow>,
    query: String,
    scope: SearchScope,
): GroupsSearchResults {
    val typed = TypoTolerantQuery.of(query)
    if (typed.isEmpty) return GroupsSearchResults.none
    val byId = groups.associateBy { it.group.id }
    // Why: the home's order, the most recent activity first, which the rows already have.
    val ordered = rows.mapNotNull { row -> byId[row.id]?.let { row to it } }
    // Why: in "Tudo" the settled groups' bills already came as their groups.
    val billGroups = ordered
        .filter { (row, _) -> scope == SearchScope.BILLS || !row.isSettled }
        .map { (_, item) -> item }

    fun sectionsOf(pass: TypoTolerantQuery): SectionHits = SectionHits(
        groups = if (scope.isLookingAt(SearchScope.GROUPS)) groupHitsOf(ordered, pass) else emptyList(),
        people = if (scope.isLookingAt(SearchScope.PEOPLE)) personHitsOf(ordered, pass) else emptyList(),
        bills = if (scope.isLookingAt(SearchScope.BILLS)) billHitsOf(billGroups, pass) else emptyList(),
    )

    // Why: what holds the text typed is what was looked for; only when nothing does, what comes close, under "Did you mean".
    val exact = sectionsOf(typed.toExactOnly())
    if (!exact.isEmpty) {
        return GroupsSearchResults(
            groups = exact.groups.map { it.hit },
            contacts = contactHitsOf(exact.people, ordered),
            people = exact.people.map { it.hit },
            bills = exact.bills.map { it.hit },
            suggestion = null,
        )
    }
    val close = sectionsOf(typed)
    val closeHits: List<Searched<*>> = close.groups + close.people + close.bills
    val people = close.people.sortedBy { it.typos }
    return GroupsSearchResults(
        groups = close.groups.toClosestFirst(),
        contacts = contactHitsOf(people, ordered),
        people = people.map { it.hit },
        bills = close.bills.toClosestFirst(),
        suggestion = closeHits.minByOrNull { it.typos }?.foundText,
    )
}

// Why: each section's hits for one pass, the exact one or, when it finds nothing, the one forgiving typos.
private class SectionHits(
    val groups: List<Searched<SearchGroupHit>>,
    val people: List<Searched<SearchPersonHit>>,
    val bills: List<Searched<SearchBillHit>>,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty() && people.isEmpty() && bills.isEmpty()
}

private fun SearchScope.isLookingAt(section: SearchScope): Boolean = this == SearchScope.ALL || this == section

// Why: a hit with how close it came, and the name or description as written where it matched, for "Did you mean".
private class Searched<T>(
    val hit: T,
    val typos: Int,
    val foundText: String,
) {
    val isExact: Boolean
        get() = typos == 0
}

private fun <T> searchedOf(
    hit: T,
    text: String,
    found: TypoTolerantQuery.Found,
): Searched<T> = Searched(
    hit = hit,
    typos = found.typos,
    foundText = text.substring(found.start, found.end),
)

private fun TypoTolerantQuery.Found.toTextMatch(): TextMatch = TextMatch(
    start = start,
    end = end,
)

// Why: sortedBy is stable, so each section keeps its own order among hits as close.
private fun <T> List<Searched<T>>.toClosestFirst(): List<T> = sortedBy { it.typos }.map { it.hit }

private fun groupHitsOf(
    groups: List<Pair<GroupRow, GroupWithMoney>>,
    typed: TypoTolerantQuery,
): List<Searched<SearchGroupHit>> = groups.mapNotNull { (row, item) ->
    val byName = typed.findIn(row.name)?.let { found ->
        searchedOf(
            hit = SearchGroupHit(
                group = row,
                nameMatch = found.toTextMatch(),
                billDescription = null,
                billMatch = null,
            ),
            text = row.name,
            found = found,
        )
    }
    // Why: a settled group also comes by a bill, when one holds the text typed closer than its name; the name on a tie.
    val byBill = if (row.isSettled && byName?.isExact != true) closestBillHitOf(row, item, typed) else null
    listOfNotNull(byName, byBill).minByOrNull { it.typos }
}

// Why: the settled group's bill closest to the text typed, the most recent of those as close.
private fun closestBillHitOf(
    row: GroupRow,
    item: GroupWithMoney,
    typed: TypoTolerantQuery,
): Searched<SearchGroupHit>? = item.expenses
    .mapNotNull { expense -> typed.findIn(expense.description)?.let { expense to it } }
    .minWithOrNull(
        compareBy<Pair<Expense, TypoTolerantQuery.Found>> { (_, found) -> found.typos }
            .thenByDescending { (expense, _) -> expense.spentAt },
    )?.let { (expense, found) ->
        searchedOf(
            hit = SearchGroupHit(
                group = row,
                nameMatch = null,
                billDescription = expense.description,
                billMatch = found.toTextMatch(),
            ),
            text = expense.description,
            found = found,
        )
    }

private fun personHitsOf(
    groups: List<Pair<GroupRow, GroupWithMoney>>,
    typed: TypoTolerantQuery,
): List<Searched<SearchPersonHit>> = groups.flatMap { (_, item) ->
    val group = item.group
    group.members
        .filter { !it.isMe }
        .mapNotNull { member ->
            typed.findIn(member.name)?.let { found ->
                searchedOf(
                    hit = SearchPersonHit(
                        memberId = member.id,
                        personKey = PeopleBalances.keyOf(member),
                        name = member.name,
                        nameMatch = found.toTextMatch(),
                        initial = Initials.firstOf(member.name),
                        color = member.avatarColor,
                        photoUrl = member.photoUrl,
                        groupId = group.id,
                        groupName = group.name,
                    ),
                    text = member.name,
                    found = found,
                )
            }
        }
}

/*
 * Why: the person once, as their first row found them (the closest, people coming closest first), with their groups
 * in common; nobody Contacts doesn't know (only in groups this device's user is no longer in), whose profile is none.
 */
private fun contactHitsOf(
    people: List<Searched<SearchPersonHit>>,
    groups: List<Pair<GroupRow, GroupWithMoney>>,
): List<SearchContactHit> {
    if (people.isEmpty()) return emptyList()
    val groupCounts = groupCountsOf(groups)
    return people
        .distinctBy { it.hit.personKey }
        .mapNotNull { row ->
            val person = row.hit
            val groupCount = groupCounts[person.personKey] ?: return@mapNotNull null
            SearchContactHit(
                personKey = person.personKey,
                name = person.name,
                nameMatch = person.nameMatch,
                initial = person.initial,
                color = person.color,
                photoUrl = person.photoUrl,
                groupCount = groupCount,
            )
        }
}

// Why: the groups in common with each person as Contacts counts them (ContactBook): the ones this device's user is in.
private fun groupCountsOf(groups: List<Pair<GroupRow, GroupWithMoney>>): Map<String, Int> = groups
    .filter { (_, item) -> item.group.members.any { it.isMe } }
    .flatMap { (_, item) ->
        item.group.members
            .filter { !it.isMe }
            .map(PeopleBalances::keyOf)
            .distinct()
    }.groupingBy { it }
    .eachCount()

private fun billHitsOf(
    groups: List<GroupWithMoney>,
    typed: TypoTolerantQuery,
): List<Searched<SearchBillHit>> = groups
    .flatMap { item ->
        val payersOf = PayersOfGroup(item.group)
        item.expenses.mapNotNull { expense ->
            typed.findIn(expense.description)?.let { found ->
                searchedOf(
                    hit = billHitOf(item.group, expense, found.toTextMatch(), payersOf),
                    text = expense.description,
                    found = found,
                )
            }
        }
    }.sortedByDescending { it.hit.spentAt }

// Why: the group's names and payer order, built once per group rather than once per bill found.
internal class PayersOfGroup(
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
    currency = expense.currency,
    payers = payersOf.of(expense),
    spentAt = expense.spentAt,
)

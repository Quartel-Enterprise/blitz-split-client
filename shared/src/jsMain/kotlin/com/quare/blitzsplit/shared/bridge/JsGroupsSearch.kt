@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchContactHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchPersonHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch

// Why: the part of a text the search found, which the page marks; end excluded.
class JsTextMatch(
    val start: Int,
    val end: Int,
)

class JsSearchGroupHit(
    val group: JsGroupRow,
    // Why: null when a bill of the settled group brought it, "· despesa “Churros”".
    val nameMatch: JsTextMatch?,
    val billDescription: String?,
    val billMatch: JsTextMatch?,
)

// Why: a person found once, opening their profile; personKey is PeopleBalances.keyOf.
class JsSearchContactHit(
    val personKey: String,
    val name: String,
    val nameMatch: JsTextMatch,
    val initial: String,
    val color: String,
    val photoUrl: String?,
    val groupCount: Int,
)

class JsSearchPersonHit(
    val memberId: String,
    val name: String,
    val nameMatch: JsTextMatch,
    val initial: String,
    val color: String,
    val photoUrl: String?,
    val groupId: String,
    val groupName: String,
)

// Why: one of whoever paid a bill found; a null name is someone who left the group.
class JsSearchBillPayer(
    val name: String?,
    val isMe: Boolean,
)

class JsSearchBillHit(
    val id: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val descriptionMatch: JsTextMatch,
    val totalMinor: Double,
    val currency: String,
    val fractionDigits: Int,
    val payers: Array<JsSearchBillPayer>,
)

// Why: "Buscar" (⌘K): what was typed, where it looks ("ALL", "GROUPS", "PEOPLE" or "BILLS") and what it found.
class JsGroupsSearch(
    val isOpen: Boolean,
    val query: String,
    val trimmedQuery: String,
    val scope: String,
    val hasQuery: Boolean,
    val showsResults: Boolean,
    val showsNoResults: Boolean,
    // Why: "Did you mean “Larissa”?" above what only comes close, null while anything holds the text typed.
    val suggestion: String?,
    val groups: Array<JsSearchGroupHit>,
    val contacts: Array<JsSearchContactHit>,
    val people: Array<JsSearchPersonHit>,
    val bills: Array<JsSearchBillHit>,
)

internal fun toJsGroupsSearch(search: GroupsSearch): JsGroupsSearch = JsGroupsSearch(
    isOpen = search.isOpen,
    query = search.query,
    trimmedQuery = search.trimmedQuery,
    scope = search.scope.name,
    hasQuery = search.hasQuery,
    showsResults = search.showsResults,
    showsNoResults = search.showsNoResults,
    suggestion = search.results.suggestion?.takeIf { search.showsSuggestion },
    groups = search.results.groups
        .map(::toJsSearchGroupHit)
        .toTypedArray(),
    contacts = search.results.contacts
        .map(::toJsSearchContactHit)
        .toTypedArray(),
    people = search.results.people
        .map(::toJsSearchPersonHit)
        .toTypedArray(),
    bills = search.results.bills
        .map(::toJsSearchBillHit)
        .toTypedArray(),
)

// Why: a scope by its name; one this version doesn't know is none.
internal fun searchScopeOrNull(name: String): SearchScope? = SearchScope.entries.firstOrNull { it.name == name }

private fun toJsTextMatch(match: TextMatch): JsTextMatch = JsTextMatch(match.start, match.end)

private fun toJsSearchGroupHit(hit: SearchGroupHit): JsSearchGroupHit = JsSearchGroupHit(
    group = toJsGroupRow(hit.group),
    nameMatch = hit.nameMatch?.let(::toJsTextMatch),
    billDescription = hit.billDescription,
    billMatch = hit.billMatch?.let(::toJsTextMatch),
)

private fun toJsSearchContactHit(hit: SearchContactHit): JsSearchContactHit = JsSearchContactHit(
    personKey = hit.personKey,
    name = hit.name,
    nameMatch = toJsTextMatch(hit.nameMatch),
    initial = hit.initial,
    color = hit.color.code,
    photoUrl = hit.photoUrl,
    groupCount = hit.groupCount,
)

private fun toJsSearchPersonHit(hit: SearchPersonHit): JsSearchPersonHit = JsSearchPersonHit(
    memberId = hit.memberId,
    name = hit.name,
    nameMatch = toJsTextMatch(hit.nameMatch),
    initial = hit.initial,
    color = hit.color.code,
    photoUrl = hit.photoUrl,
    groupId = hit.groupId,
    groupName = hit.groupName,
)

private fun toJsSearchBillHit(hit: SearchBillHit): JsSearchBillHit = JsSearchBillHit(
    id = hit.id,
    groupId = hit.groupId,
    groupName = hit.groupName,
    description = hit.description,
    descriptionMatch = toJsTextMatch(hit.descriptionMatch),
    totalMinor = hit.totalMinor.toDouble(),
    currency = hit.currency.code,
    fractionDigits = hit.currency.fractionDigits,
    payers = hit.payers.map { JsSearchBillPayer(it.name, it.isMe) }.toTypedArray(),
)

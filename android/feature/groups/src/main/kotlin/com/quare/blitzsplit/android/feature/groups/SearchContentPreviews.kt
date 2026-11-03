package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch

private val noResults = GroupsSearchResults(
    groups = emptyList(),
    people = emptyList(),
    bills = emptyList(),
)

@DevicePreviews
@Composable
private fun SearchEmptyPreview() {
    SearchPreviewContent(
        GroupsSearch(
            isOpen = true,
            query = "",
            scope = SearchScope.ALL,
            results = noResults,
        ),
    )
}

@DevicePreviews
@Composable
private fun SearchResultsPreview() {
    SearchPreviewContent(buildPreviewSearch())
}

@DevicePreviews
@Composable
private fun SearchNoResultsPreview() {
    SearchPreviewContent(
        GroupsSearch(
            isOpen = true,
            query = "pizza",
            scope = SearchScope.ALL,
            results = noResults,
        ),
    )
}

@Composable
private fun SearchPreviewContent(search: GroupsSearch) {
    BlitzSplitTheme {
        SearchContent(
            search = search,
            onEvent = {},
        )
    }
}

// Why: "chur" finding a group and one of its bills, which Grupos' search and Route.Search previews both draw.
internal fun buildPreviewSearch(): GroupsSearch {
    val churrasco = buildPreviewRow(
        id = "4",
        name = "Churrasco de sábado",
        color = AvatarColor.BLUE,
        kind = GroupBalanceKind.TO_RECEIVE,
        receivesMinor = 9_750L,
        owesMinor = 0L,
    )
    return GroupsSearch(
        isOpen = true,
        query = "chur",
        scope = SearchScope.ALL,
        results = GroupsSearchResults(
            groups = listOf(
                SearchGroupHit(
                    group = churrasco,
                    nameMatch = TextMatch(0, 4),
                    billDescription = null,
                    billMatch = null,
                ),
            ),
            people = emptyList(),
            bills = listOf(
                SearchBillHit(
                    id = "b",
                    groupId = "4",
                    groupName = churrasco.name,
                    description = "Carne e carvão do churrasco",
                    descriptionMatch = TextMatch(18, 22),
                    totalMinor = 48_600L,
                    currency = GroupCurrency.BRL,
                    payers = listOf(SearchBillPayer("Bruno", isMe = false)),
                    spentAt = 0L,
                ),
            ),
        ),
    )
}

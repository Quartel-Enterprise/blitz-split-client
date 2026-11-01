package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.paidBySeveralOf
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRowDetail
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchPersonHit
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Buscar" in the list's place: "Grupos", "Pessoas" and "Despesas" with what was found marked, or "Nada com
 * “pizza”". Always a list, even with the grid picked. A tablet keeps it in the list's centered column.
 */
@Composable
internal fun GroupsSearchPane(
    search: GroupsSearch,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val listModifier = Modifier
            .then(if (layout.isTablet) Modifier.widthIn(max = 560.dp) else Modifier)
            .fillMaxSize()
        when {
            search.showsNoResults -> SearchNoResultsColumn(
                query = search.trimmedQuery,
                onClear = { onEvent(GroupsUiEvent.OnSearchQueryChanged("")) },
                modifier = listModifier,
            )

            !search.hasQuery -> SearchHintText(modifier = listModifier)

            else -> LazyColumn(
                modifier = listModifier,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                searchResultItems(
                    search = search,
                    onEvent = onEvent,
                )
            }
        }
    }
}

private fun LazyListScope.searchResultItems(
    search: GroupsSearch,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    val results = search.results
    if (results.groups.isNotEmpty()) {
        item(key = "groups-header") { SearchSectionHeader(stringResource(R.string.groups_search_section_groups)) }
        itemsIndexed(results.groups, key = { _, hit -> "group-${hit.group.id}" }) { index, hit ->
            SearchGroupRow(
                hit = hit,
                shape = groupedRowShapeOf(index, results.groups.size),
                onClick = { onEvent(GroupsUiEvent.OnGroupClicked(hit.group.id)) },
            )
        }
    }
    if (results.people.isNotEmpty()) {
        item(key = "people-header") { SearchSectionHeader(stringResource(R.string.groups_search_section_people)) }
        itemsIndexed(results.people, key = { _, hit -> "person-${hit.groupId}-${hit.memberId}" }) { index, hit ->
            SearchPersonRow(
                hit = hit,
                shape = groupedRowShapeOf(index, results.people.size),
                onClick = { onEvent(GroupsUiEvent.OnSearchPersonClicked(hit.groupId)) },
            )
        }
    }
    if (results.bills.isNotEmpty()) {
        item(key = "bills-header") { SearchSectionHeader(stringResource(R.string.groups_search_section_bills)) }
        itemsIndexed(results.bills, key = { _, hit -> "bill-${hit.id}" }) { index, hit ->
            SearchBillRow(
                hit = hit,
                shape = groupedRowShapeOf(index, results.bills.size),
                onClick = { onEvent(GroupsUiEvent.OnSearchBillClicked(hit.groupId, hit.id)) },
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .padding(
                start = 4.dp,
                top = 16.dp,
                bottom = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SearchGroupRow(
    hit: SearchGroupHit,
    shape: Shape,
    onClick: () -> Unit,
) {
    val group = hit.group
    val money = rememberMoneyFormatter(group.currency)
    val dates = rememberDateTimeTextFormatter()
    val billDescription = hit.billDescription
    val subtitle = if (billDescription != null) {
        templatedTextOf(
            template = stringResource(R.string.groups_search_settled_bill),
            value = highlightedTextOf(billDescription, hit.billMatch),
        )
    } else {
        val detail = detailTextOf(
            group = group,
            amountText = money::toAmountText,
            dates = dates,
        )
        // Why: "9 membros · 3 dias"; a settled group's detail already counts its members.
        AnnotatedString(
            if (group.detail == GroupRowDetail.EVERYONE_SETTLED) {
                detail
            } else {
                stringResource(R.string.groups_search_group_meta, membersTextOf(group.memberCount), detail)
            },
        )
    }
    SearchResultRow(
        shape = shape,
        onClick = onClick,
        leading = {
            GroupBadge(
                initials = group.initials,
                color = group.color,
            )
        },
        title = highlightedTextOf(group.name, hit.nameMatch),
        subtitle = subtitle,
        trailing = {
            if (group.isSettled) {
                SettledLabel()
            } else {
                Text(
                    text = money.toSignedText(group.netMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColorOf(group.netMinor),
                    maxLines = 1,
                )
            }
        },
    )
}

@Composable
private fun SearchPersonRow(
    hit: SearchPersonHit,
    shape: Shape,
    onClick: () -> Unit,
) {
    SearchResultRow(
        shape = shape,
        onClick = onClick,
        leading = {
            AvatarBadge(
                initial = hit.initial,
                color = hit.color,
                size = 48.dp,
            )
        },
        title = highlightedTextOf(hit.name, hit.nameMatch),
        subtitle = AnnotatedString(hit.groupName),
        trailing = {},
    )
}

@Composable
private fun SearchBillRow(
    hit: SearchBillHit,
    shape: Shape,
    onClick: () -> Unit,
) {
    val money = rememberMoneyFormatter(hit.currency)
    val paidBy = paidByTextOf(hit.payers)
    SearchResultRow(
        shape = shape,
        onClick = onClick,
        leading = { SearchIconTile(Icons.AutoMirrored.Outlined.ReceiptLong) },
        title = highlightedTextOf(hit.description, hit.descriptionMatch),
        subtitle = AnnotatedString(
            paidBy?.let { stringResource(R.string.groups_search_bill_meta, hit.groupName, it) } ?: hit.groupName,
        ),
        trailing = {
            Text(
                text = money.toText(hit.totalMinor),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun SearchResultRow(
    shape: Shape,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: AnnotatedString,
    subtitle: AnnotatedString,
    trailing: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}

@Composable
private fun SearchIconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: before anything is typed, where the search looks.
@Composable
private fun SearchHintText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.groups_search_hint),
        modifier = modifier.padding(
            horizontal = 40.dp,
            vertical = 48.dp,
        ),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SearchNoResultsColumn(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            start = 40.dp,
            end = 40.dp,
            top = 72.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.groups_search_none_title, query),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.groups_search_none_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.groups_search_clear))
        }
    }
}

// Why: what the search found, marked in the tertiary container, as the design draws it.
@Composable
private fun highlightedTextOf(
    text: String,
    match: TextMatch?,
): AnnotatedString {
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    return buildAnnotatedString {
        if (match == null || match.end > text.length) {
            append(text)
            return@buildAnnotatedString
        }
        append(text.substring(0, match.start))
        withStyle(style) { append(text.substring(match.start, match.end)) }
        append(text.substring(match.end))
    }
}

// Why: a translated sentence around a marked value: "Quitado · despesa “%s”".
private fun templatedTextOf(
    template: String,
    value: AnnotatedString,
): AnnotatedString = buildAnnotatedString {
    val parts = template.split("%s", limit = 2)
    append(parts.first())
    append(value)
    if (parts.size > 1) append(parts[1])
}

// Why: "Bruno pagou", "Você pagou", "Ana e Bruno pagaram" or "3 pessoas pagaram"; null when nobody in it paid.
@Composable
private fun paidByTextOf(payers: List<SearchBillPayer>): String? {
    val you = stringResource(R.string.groups_search_you)
    val formerMember = stringResource(DesignR.string.designsystem_former_member)
    val lonePayer = payers.singleOrNull()
    return when {
        payers.isEmpty() -> null
        lonePayer?.isMe == true -> stringResource(R.string.groups_search_you_paid)
        lonePayer != null -> lonePayer.name?.let { stringResource(DesignR.string.designsystem_paid_by_one, it) }
        else -> paidBySeveralOf(payers.map { if (it.isMe) you else it.name ?: formerMember })
    }
}

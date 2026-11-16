package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.opensProfile
import com.quare.blitzsplit.android.designsystem.component.rememberProfileFlightSource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toDataColor
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val ringSize = 160.dp

/*
 * Why: "Por pessoa": Consumo or Pagou, a ring of each person's share in their own color with the largest in its middle,
 * the people under it as its legend (each one's dot, share and amount), and what each view means. A slice tapped, or
 * a slice or a row the mouse is over, pops out and takes the middle; a row still opens the person's profile.
 */
@Composable
internal fun StatsPeopleSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val views = listOf(StatsPeopleView.CONSUMPTION, StatsPeopleView.PAID)
    val people = state.people
    var selected by remember { mutableStateOf<Int?>(null) }
    // Why: the same keys as iOS and the web: the period, the view and each person's share.
    val shares = people.map { it.memberId to it.percent }
    val ringKey = listOf(state.periodStartMillis, state.periodEndMillis, state.peopleView, shares)
    LaunchedEffect(ringKey) { selected = null }
    val hasShares = state.topPerson != null
    /*
     * Why: a row's hover keeps running across recompositions, so it reads whether anyone has a share now; a row at 0%
     * has no slice to pick.
     */
    val currentHasShares by rememberUpdatedState(hasShares)
    val picked = selected?.takeIf { hasShares && it in people.indices }
    Column(modifier = modifier) {
        StatsSectionHeading(stringResource(R.string.group_stats_by_person), modifier = sectionPadding)
        ConnectedButtonRow(
            labels = listOf(
                stringResource(R.string.group_stats_consumption),
                stringResource(R.string.group_stats_paid),
            ),
            selectedIndex = views.indexOf(state.peopleView),
            groupLabel = stringResource(R.string.group_stats_show_by_person),
            onClick = { index -> onEvent(GroupStatsUiEvent.OnPeopleViewPicked(views[index])) },
            height = 40.dp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        StatsPeopleRingCard(
            state = state,
            selected = picked,
            ringKey = ringKey,
            size = ringSize,
            onPick = { selected = it },
        )
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            people.forEachIndexed { index, person ->
                val shape = groupedRowShapeOf(index, people.size)
                // Why: keyed by the person, so a face's id stays with them when the view reorders the list.
                key(person.memberId) {
                    // Why: the row opens their profile (Perfil for this device's user), its face flying into it; an ex-member has none.
                    val flight = rememberProfileFlightSource()
                    StatsPersonRowItem(
                        person = person,
                        money = money,
                        modifier = Modifier
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
                            .onMouseHover(
                                key = index,
                                onMove = { if (currentHasShares && person.percent > 0) selected = index },
                                onExit = { selected = null },
                            ).alpha(if (picked == null || picked == index) 1f else RING_DIMMED_ALPHA)
                            .opensProfile(
                                flight.toFlyingClickOrNull(
                                    {
                                        onEvent(
                                            GroupStatsUiEvent.OnPersonClicked(person.memberId),
                                        )
                                    }.takeIf { person.opensProfile },
                                ),
                            ),
                        faceModifier = flight.face,
                    )
                }
            }
        }
        val showsPaid = state.peopleView == StatsPeopleView.PAID
        Text(
            text = stringResource(
                if (showsPaid) R.string.group_stats_note_paid else R.string.group_stats_note_consumption,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 4.dp,
                end = 4.dp,
                top = 12.dp,
            ),
        )
    }
}

// Why: the ring of "Por pessoa" on its card, centered; a finger's tap picks a slice and a second one lets it go.
@Composable
private fun StatsPeopleRingCard(
    state: GroupStatsUiState,
    selected: Int?,
    ringKey: Any,
    size: Dp,
    onPick: (Int?) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val people = state.people
    val names = people.map { personName(it) }
    val parts = people.mapIndexed { index, person ->
        stringResource(R.string.group_stats_category_pie_part, names[index], person.percent)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        StatsRingDisplay(
            slices = people.map { StatsRingSlice(it.percent, it.color.toDataColor(isDark)) },
            selected = selected,
            sweepKey = ringKey,
            description = stringResource(R.string.group_stats_person_pie, parts.joinToString(", ")),
            size = size,
            onPick = onPick,
            onTap = { index, isMouse ->
                if (!isMouse) onPick(index.takeIf { it != selected })
            },
        ) {
            val shown = selected ?: 0
            state.topPerson?.let {
                StatsRingLabel(
                    key = people[shown].memberId,
                    percent = people[shown].percent,
                    label = names[shown],
                )
            }
        }
    }
}

// Why: "Pierre (você)" for this device's user, "Ex-membro" for someone who left.
@Composable
private fun personName(person: StatsPersonRow): String {
    val formerMember = stringResource(DesignR.string.designsystem_former_member)
    return person.name?.let { if (person.isMe) stringResource(R.string.group_stats_you, it) else it } ?: formerMember
}

// Why: a line of the ring's legend: the face, the slice's color before the name, the share and the amount.
@Composable
private fun StatsPersonRowItem(
    person: StatsPersonRow,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
    faceModifier: Modifier = Modifier,
) {
    val name = personName(person)
    val amount = money.toText(person.amountMinor)
    val share = stringResource(R.string.group_stats_category_share, name, person.percent)
    StatsAvatarRow(
        initials = person.initials,
        avatarColor = person.avatarColor,
        photoUrl = person.photoUrl,
        description = "$share, $amount",
        modifier = modifier,
        faceModifier = faceModifier,
    ) {
        val hasAmount = person.amountMinor > 0
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(person.color.toDataColor(LocalIsDarkTheme.current), CircleShape),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.group_stats_category_percent, person.percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            softWrap = false,
            maxLines = 1,
            color = if (hasAmount) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

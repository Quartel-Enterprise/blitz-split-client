package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AmountText
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.amountColorOf
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import software.quare.loadable.valueOrNull
import kotlin.math.abs
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Saldos com pessoas", each person once per currency, netted across the groups. HomePeopleLayout picks by the
 * section's width and the count: rows (face, name, amount and "te deve" on one line) while they take two lines at most,
 * else the row of faces, whose count joins the title once it scrolls. While it is read, or the first download fills
 * the device, a shimmer holds its place; with everyone settled the section goes, leaving no gap.
 */
@Composable
internal fun HomePeopleSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (state.isLoadingPeople) {
        HomePeopleSkeleton(metrics)
        return
    }
    val people = state.people.valueOrNull().orEmpty()
    if (people.isEmpty()) return
    val listState = rememberLazyListState()
    BoxWithConstraints {
        val layout = HomePeopleLayout.of(
            peopleCount = people.size,
            sectionWidth = (maxWidth - metrics.gutter * 2).value,
        )
        val scrolls = !layout.showsRows && (listState.canScrollForward || listState.canScrollBackward)
        val onPerson = { person: HomePerson -> onEvent(HomeUiEvent.OnPersonClicked(person.key, person.currency)) }
        Column {
            HomeSectionHeader(
                title = if (scrolls) {
                    stringResource(R.string.groups_home_people_title_count, people.size)
                } else {
                    stringResource(R.string.groups_home_people_title)
                },
                action = stringResource(R.string.groups_home_see_balances),
                onAction = { onEvent(HomeUiEvent.OnSeeBalancesClicked) },
                modifier = Modifier.padding(horizontal = metrics.gutter),
            )
            if (layout.showsRows) {
                PeopleRowsColumn(
                    people = people,
                    columns = layout.columns,
                    onPerson = onPerson,
                    modifier = Modifier.padding(horizontal = metrics.gutter),
                )
            } else {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = metrics.gutter),
                    horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompact) 6.dp else 8.dp),
                ) {
                    items(
                        people,
                        key = { person -> person.key + person.currency.code },
                    ) { person ->
                        PersonFaceItem(
                            person = person,
                            size = metrics.faceSize,
                            onClick = { onPerson(person) },
                        )
                    }
                }
            }
        }
    }
}

// Why: the rows in HomePeopleLayout's columns, read as one card in one column as Últimas despesas' rows are.
@Composable
private fun PeopleRowsColumn(
    people: List<HomePerson>,
    columns: Int,
    onPerson: (HomePerson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = people.chunked(columns)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEachIndexed { lineIndex, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(columns) { column ->
                    val person = line.getOrNull(column)
                    if (person == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        PersonRowItem(
                            person = person,
                            shape = if (columns == 1) {
                                groupedRowShapeOf(
                                    index = lineIndex,
                                    count = lines.size,
                                )
                            } else {
                                MaterialTheme.shapes.large
                            },
                            onClick = { onPerson(person) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// Why: a row of "Saldos com pessoas": the face, the name, the amount and "te deve" or "você deve" on one line.
@Composable
private fun PersonRowItem(
    person: HomePerson,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(person.currency)
    val texts = personTextsOf(person)
    PersonTooltipBox(
        person = person,
        name = texts.name,
    ) {
        // Why: the Surface clips the ripple to the row's shape; the click sits before the semantics it keeps.
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .clearAndSetSemantics {
                        contentDescription = texts.description
                        role = Role.Button
                    }.heightIn(min = 56.dp)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonFaceBadge(
                    person = person,
                    size = 40.dp,
                )
                Text(
                    text = texts.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmountText(
                        text = money.toSignedText(person.netMinor),
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColorOf(person.netMinor),
                    )
                    Text(
                        text = texts.word,
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Why: a person's name ("Ex-membro" for someone gone), "te deve" or "você deve", and what TalkBack reads for them.
private class PersonTexts(
    val name: String,
    val word: String,
    val description: String,
)

@Composable
private fun personTextsOf(person: HomePerson): PersonTexts {
    val money = rememberMoneyFormatter(person.currency)
    val name = person.name ?: stringResource(DesignR.string.designsystem_former_member)
    val owesYou = person.netMinor > 0
    return PersonTexts(
        name = name,
        word = stringResource(
            if (owesYou) R.string.groups_home_person_owes_you else R.string.groups_home_person_you_owe,
        ),
        description = stringResource(
            when {
                owesYou && person.isGuest -> R.string.groups_home_guest_owes_you_description
                owesYou -> R.string.groups_home_person_owes_you_description
                person.isGuest -> R.string.groups_home_guest_you_owe_description
                else -> R.string.groups_home_person_you_owe_description
            },
            name,
            money.toText(abs(person.netMinor)),
        ),
    )
}

// Why: the face, a guest's wearing the crossed-out phone.
@Composable
private fun PersonFaceBadge(
    person: HomePerson,
    size: Dp,
) {
    Box {
        AvatarBadge(
            initial = person.initials,
            color = person.color ?: AvatarColor.default,
            photoUrl = person.photoUrl,
            size = size,
        )
        if (person.isGuest) GuestBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

// Why: a guest, a name nobody joined as, gets no notice; a long press on them says so.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonTooltipBox(
    person: HomePerson,
    name: String,
    content: @Composable () -> Unit,
) {
    if (!person.isGuest) {
        content()
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text(
                    stringResource(
                        if (person.netMinor > 0) {
                            R.string.groups_home_guest_owes_you_tip
                        } else {
                            R.string.groups_home_guest_you_owe_tip
                        },
                        name,
                    ),
                )
            }
        },
        state = rememberTooltipState(),
        content = content,
    )
}

/*
 * Why: a face, what tapping it settles up (4.5) or records as received (6.5). A guest, a name nobody joined as, wears
 * the crossed-out phone, and a long press says they get no notice.
 */
@Composable
private fun PersonFaceItem(
    person: HomePerson,
    size: Dp,
    onClick: () -> Unit,
) {
    val money = rememberMoneyFormatter(person.currency)
    val texts = personTextsOf(person)
    PersonTooltipBox(
        person = person,
        name = texts.name,
    ) {
        Column(
            modifier = Modifier
                .width(size + 20.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = texts.description
                    role = Role.Button
                }.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PersonFaceBadge(
                person = person,
                size = size,
            )
            Text(
                text = texts.name,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AmountText(
                text = money.toSignedText(person.netMinor),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = amountColorOf(person.netMinor),
            )
            Text(
                text = texts.word,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuestBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MobileOff,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MobileOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import kotlin.math.abs
import com.quare.blitzsplit.android.designsystem.R as DesignR

private const val SKELETON_FACES = 5

// Why: "Saldos com pessoas", "Seus grupos" and "Últimas despesas", each with the link that opens all of it.
@Composable
internal fun HomeSectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAction) { Text(action) }
    }
}

/*
 * Why: "Saldos com pessoas", each person once per currency, netted across the groups; the count joins the title once
 * the row scrolls. While the first download fills the device, grey faces hold their place; with everyone settled the
 * row goes, leaving no gap.
 */
@Composable
internal fun HomePeopleSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (!state.showsPeople) {
        if (state.isFirstDownload) PeopleSkeletonRow(metrics)
        return
    }
    val listState = rememberLazyListState()
    val scrolls = listState.canScrollForward || listState.canScrollBackward
    Column {
        HomeSectionHeader(
            title = if (scrolls) {
                stringResource(R.string.groups_home_people_title_count, state.people.size)
            } else {
                stringResource(R.string.groups_home_people_title)
            },
            action = stringResource(R.string.groups_home_see_balances),
            onAction = { onEvent(HomeUiEvent.OnSeeBalancesClicked) },
            modifier = Modifier.padding(horizontal = metrics.gutter),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = metrics.gutter),
            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompact) 6.dp else 8.dp),
        ) {
            items(
                state.people,
                key = { person -> person.key + person.currency.code },
            ) { person ->
                PersonFaceItem(
                    person = person,
                    size = metrics.faceSize,
                    onClick = { onEvent(HomeUiEvent.OnPersonClicked(person.key, person.currency)) },
                )
            }
        }
    }
}

/*
 * Why: a face, what tapping it settles up (4.5) or records as received (6.5). A guest, a name nobody joined as, wears
 * the crossed-out phone, and a long press says they get no notice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonFaceItem(
    person: HomePerson,
    size: Dp,
    onClick: () -> Unit,
) {
    val money = rememberMoneyFormatter(person.currency)
    val name = person.name ?: stringResource(DesignR.string.designsystem_former_member)
    val amount = money.toText(abs(person.netMinor))
    val owesYou = person.netMinor > 0
    val description = stringResource(
        when {
            owesYou && person.isGuest -> R.string.groups_home_guest_owes_you_description
            owesYou -> R.string.groups_home_person_owes_you_description
            person.isGuest -> R.string.groups_home_guest_you_owe_description
            else -> R.string.groups_home_person_you_owe_description
        },
        name,
        amount,
    )
    val face: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .width(size + 20.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                AvatarBadge(
                    initial = person.initials,
                    color = person.color ?: AvatarColor.default,
                    size = size,
                )
                if (person.isGuest) GuestBadge(modifier = Modifier.align(Alignment.BottomEnd))
            }
            Text(
                text = name,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = money.toSignedText(person.netMinor),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = amountColorOf(person.netMinor),
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    if (owesYou) R.string.groups_home_person_owes_you else R.string.groups_home_person_you_owe,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!person.isGuest) {
        face()
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text(
                    stringResource(
                        if (owesYou) {
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
    ) { face() }
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

@Composable
private fun PeopleSkeletonRow(metrics: HomeMetrics) {
    Row(
        modifier = Modifier
            .padding(
                horizontal = metrics.gutter,
                vertical = 12.dp,
            ).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(SKELETON_FACES) {
            Column(
                modifier = Modifier.width(metrics.faceSize + 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(
                    width = metrics.faceSize,
                    height = metrics.faceSize,
                    shape = CircleShape,
                )
                SkeletonBox(width = 48.dp)
                SkeletonBox(width = 60.dp)
            }
        }
    }
}

// Why: the grey shape something takes while the first download brings it.
@Composable
internal fun SkeletonBox(
    width: Dp?,
    height: Dp = 10.dp,
    shape: Shape = RoundedCornerShape(height / 2),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape),
    )
}

/*
 * Why: "Saldo geral", the net already netted across the people (Grupos' Saldo geral), big in the first currency with a
 * net and a line for each other one. "Te devem" and "Você deve" open Saldos filtered, disabled at zero.
 */
@Composable
internal fun HomeSummaryCard(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val main = summary.main
    val currency = main?.currency ?: rememberRegionCurrency()
    val money = rememberMoneyFormatter(currency)
    // Why: each button in the first currency with something that way, the big amount's zero when there is nothing.
    val toReceive = summary.toReceive
    val toPay = summary.toPay
    val toReceiveMoney = rememberMoneyFormatter(toReceive?.currency ?: currency)
    val toPayMoney = rememberMoneyFormatter(toPay?.currency ?: currency)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (metrics.isCompact) 18.dp else 24.dp,
                end = if (metrics.isCompact) 12.dp else 16.dp,
                top = if (metrics.isCompact) 14.dp else 20.dp,
                bottom = if (metrics.isCompact) 12.dp else 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.groups_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = money.toSignedText(main?.netMinor ?: 0L),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (metrics.isCompact) 26.sp else 32.sp,
                        lineHeight = if (metrics.isCompact) 34.sp else 40.sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    softWrap = false,
                    maxLines = 1,
                    // Why: "+R$ 15.600,00" shrinks to fit beside the buttons rather than break after its sign.
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 16.sp,
                        maxFontSize = if (metrics.isCompact) 26.sp else 32.sp,
                    ),
                )
                ExtraCurrencyText(totals = summary.others)
                Text(
                    text = stringResource(summaryHintOf(state)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(
                modifier = Modifier.width(148.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SummaryButton(
                    label = stringResource(
                        if (summary.canOpenToReceive) {
                            R.string.groups_home_to_receive
                        } else {
                            R.string.groups_home_nothing_to_receive
                        },
                    ),
                    // Why: "Nada a receber" is read as it shows, never as a link to Te devem it can't open.
                    descriptionOf = { amount ->
                        if (summary.canOpenToReceive) {
                            stringResource(R.string.groups_home_to_receive_description, amount)
                        } else {
                            stringResource(R.string.groups_home_nothing_to_receive)
                        }
                    },
                    amounts = listOf(toReceiveMoney.toText(toReceive?.toReceiveMinor ?: 0L)),
                    amountColor = MaterialTheme.colorScheme.primary,
                    isEnabled = summary.canOpenToReceive,
                    onClick = { onEvent(HomeUiEvent.OnToReceiveClicked) },
                )
                SummaryButton(
                    label = stringResource(
                        if (summary.canOpenToPay) R.string.groups_home_to_pay else R.string.groups_home_nothing_to_pay,
                    ),
                    descriptionOf = { amount ->
                        if (summary.canOpenToPay) {
                            stringResource(R.string.groups_home_to_pay_description, amount)
                        } else {
                            stringResource(R.string.groups_home_nothing_to_pay)
                        }
                    },
                    amounts = listOf(toPayMoney.toText(toPay?.toPayMinor ?: 0L)),
                    amountColor = MaterialTheme.colorScheme.error,
                    isEnabled = summary.canOpenToPay,
                    onClick = { onEvent(HomeUiEvent.OnToPayClicked) },
                )
            }
        }
    }
}

// Why: "+US$ 40,00" under the big amount, for each other currency with a net; never added to the first.
@Composable
private fun ExtraCurrencyText(totals: List<PeopleTotal>) {
    val locale = LocalLocale.current.platformLocale
    totals.forEach { total ->
        val money = remember(total.currency, locale) { MoneyFormatter(total.currency, locale) }
        Text(
            text = money.toSignedText(total.netMinor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun summaryHintOf(state: HomeUiState): Int = when {
    state.isFirstDownload -> R.string.groups_home_summary_first_download
    state.summary.kind == GroupBalanceKind.TO_RECEIVE -> R.string.groups_home_summary_to_receive_hint
    state.summary.kind == GroupBalanceKind.YOU_OWE -> R.string.groups_home_summary_to_pay_hint
    else -> R.string.groups_home_summary_settled_hint
}

// Why: before any group arrives (the first download) there is no currency yet; the device's region gives one.
@Composable
internal fun rememberRegionCurrency(): GroupCurrency {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { GroupCurrency.ofRegion(locale.country.ifEmpty { null }) }
}

// Why: the profile's avatar at the end of the top bar, or of the first column's header beside the rail.
@Composable
internal fun ProfileAvatarButton(
    initial: String,
    color: AvatarColor,
    photoUrl: String?,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        val profileLabel = stringResource(R.string.groups_profile)
        AvatarBadge(
            initial = initial,
            color = color,
            modifier = Modifier.semantics { contentDescription = profileLabel },
            photoUrl = photoUrl,
        )
    }
}

// Why: room under the last row for what floats over it (the FAB), or the pane's own bottom margin.
@Composable
internal fun BottomClearanceSpacer(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

// Why: the chip, and under it the first download's bar, which can't count the groups still to come.
@Composable
internal fun HomeSyncStatusIndicator(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.syncBadge == null && !state.isFirstDownload) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SyncBadgeChip(
            badge = state.syncBadge,
            pendingChangeCount = state.pendingChangeCount,
            canRetry = state.canRetrySync,
            onRetry = { onEvent(HomeUiEvent.OnRetrySyncClicked) },
        )
        if (state.isFirstDownload) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

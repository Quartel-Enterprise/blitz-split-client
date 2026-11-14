package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.AmountText
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.theme.positiveAmountColor
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import software.quare.loadable.valueOrNull
import com.quare.blitzsplit.android.designsystem.component.ProfileAvatarButton as DesignProfileAvatarButton

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
    val summary = state.summary.valueOrNull() ?: run {
        HomeSummarySkeleton(
            metrics = metrics,
            modifier = modifier,
        )
        return
    }
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
                    text = stringResource(summaryHintOf(state, summary)),
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
                    amountColor = positiveAmountColor(),
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
    totals.forEach { total ->
        val money = rememberMoneyFormatter(total.currency)
        AmountText(
            text = money.toSignedText(total.netMinor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun summaryHintOf(
    state: HomeUiState,
    summary: HomeSummary,
): Int = when {
    state.isFirstDownload -> R.string.groups_home_summary_first_download
    summary.kind == GroupBalanceKind.TO_RECEIVE -> R.string.groups_home_summary_to_receive_hint
    summary.kind == GroupBalanceKind.YOU_OWE -> R.string.groups_home_summary_to_pay_hint
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
    DesignProfileAvatarButton(
        initial = initial,
        color = color,
        photoUrl = photoUrl,
        contentDescription = stringResource(R.string.groups_profile),
        onClick = onClick,
    )
}

// Why: room under the last row for what floats over it (the FAB), or the pane's own bottom margin.
@Composable
internal fun BottomClearanceSpacer(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

/*
 * Why: the chip alone, in the top bar's empty leading area (beside the title on a tablet), so it comes and goes
 * without pushing the content down.
 */
@Composable
internal fun HomeSyncChip(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
) {
    SyncBadgeChip(
        badge = state.syncBadge,
        pendingChangeCount = state.pendingChangeCount,
        canRetry = state.canRetrySync,
        onRetry = { onEvent(HomeUiEvent.OnRetrySyncClicked) },
        downloadProgress = state.firstDownload,
        isSingleLine = true,
    )
}

// Why: the first download's bar, which fills as the groups come ("3 de 7 grupos"), under the top bar's chip.
@Composable
internal fun HomeDownloadStatusLine(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    state.firstDownload?.let {
        DownloadStatusLine(
            progress = it,
            modifier = modifier,
        )
    }
}

// Why: the chip, and under it the first download's bar, which fills as the groups come ("3 de 7 grupos").
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
            downloadProgress = state.firstDownload,
        )
        HomeDownloadStatusLine(state = state)
    }
}

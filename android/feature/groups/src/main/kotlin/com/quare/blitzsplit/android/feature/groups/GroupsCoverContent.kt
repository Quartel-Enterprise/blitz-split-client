package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.rememberCurrencySymbol
import com.quare.blitzsplit.android.designsystem.component.toCurrencyFontSize
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBill
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBillAuthor
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import kotlinx.coroutines.launch

/*
 * Why: the home on a Flip's cover screen (5.1, 360×374): "Saldo geral", the latest bill saved and "Abrir no celular".
 * No list, search nor form, which need the phone open; the app can't open it, so the button says to.
 */
@Composable
internal fun GroupsCoverContent(
    state: GroupsUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val openPhoneHint = stringResource(R.string.groups_cover_open_phone_hint)
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverBrandRow(isOffline = state.syncBadge == SyncBadge.OFFLINE)
            CoverBalanceCard(state = state)
            state.latestBill?.let { bill -> CoverLatestBillSection(bill = bill) }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { scope.launch { snackbarHostState.showSnackbar(openPhoneHint) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.groups_cover_open_phone),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// Why: the brand badge (the region's currency symbol), the app's name and, offline, "Offline".
@Composable
private fun CoverBrandRow(isOffline: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val symbol = rememberCurrencySymbol()
        Surface(
            modifier = Modifier.size(22.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = symbol,
                    fontSize = toCurrencyFontSize(symbol, 9.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = stringResource(R.string.groups_cover_brand),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isOffline) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.groups_cover_offline),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Saldo geral", one amount per currency as the home's card, and "a receber em 4 grupos".
@Composable
private fun CoverBalanceCard(state: GroupsUiState) {
    val summary = state.summary
    val locale = LocalLocale.current.platformLocale
    val formatters = remember(summary.totals, locale) {
        summary.totals.associate { it.currency to MoneyFormatter(it.currency, locale) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.groups_summary_title),
                style = MaterialTheme.typography.labelLarge,
            )
            amountsOf(summary.totals, BalanceTotal::netMinor).forEach { (currency, minor) ->
                Text(
                    text = formatters.getValue(currency).toSignedText(minor),
                    style = if (summary.totals.size > 1) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineLarge
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Text(
                text = when (summary.kind) {
                    GroupBalanceKind.TO_RECEIVE -> pluralStringResource(
                        R.plurals.groups_cover_to_receive,
                        summary.toReceiveGroupCount,
                        summary.toReceiveGroupCount,
                    )

                    GroupBalanceKind.YOU_OWE -> pluralStringResource(
                        R.plurals.groups_cover_to_pay,
                        summary.toPayGroupCount,
                        summary.toPayGroupCount,
                    )

                    else -> stringResource(R.string.groups_summary_settled_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Why: "Última atividade": "Ana lançou Uber aeroporto, R$ 87,60, em Viagem Floripa".
@Composable
private fun CoverLatestBillSection(bill: LatestBill) {
    val locale = LocalLocale.current.platformLocale
    val amount = remember(bill, locale) { MoneyFormatter(bill.currency, locale).toText(bill.amountMinor) }
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.groups_cover_last_activity),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = when (val author = bill.savedBy) {
                LatestBillAuthor.Me ->
                    stringResource(R.string.groups_cover_latest_by_me, bill.description, amount, bill.groupName)

                is LatestBillAuthor.Member -> stringResource(
                    R.string.groups_cover_latest_by,
                    author.name,
                    bill.description,
                    amount,
                    bill.groupName,
                )

                LatestBillAuthor.Unknown ->
                    stringResource(R.string.groups_cover_latest, bill.description, amount, bill.groupName)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

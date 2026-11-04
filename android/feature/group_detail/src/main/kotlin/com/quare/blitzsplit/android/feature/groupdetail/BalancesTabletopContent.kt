package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.MyBalanceCard
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.TabletopGap
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

/*
 * Why: Saldos on a tabletop (6.2), the group drawn alone: a one-line bar, then the tabs and "Seu saldo neste grupo" in
 * the top half, facing whoever reads, and the pairs with Acertar, Recebi and Já pagou in the bottom one, within thumb
 * reach, "Com você" beside "Entre os outros" while both have a pair. No "Rachar conta", as the board draws it.
 */
@Composable
internal fun BalancesTabletopContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState,
    money: MoneyFormatter,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (state.isLoaded) {
                GroupCompactTopBar(
                    header = state.header,
                    isOnline = state.exit.isOnline,
                    onAction = onAction,
                )
            } else {
                GroupLoadingTopBar(onAction)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TopBottomHalvesLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            gap = TabletopGap,
            top = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GroupTabsRow(
                        selectedTab = state.tab,
                        onAction = onAction,
                    )
                    val myBalance = state.header.myBalance
                    if (state.isMyBalanceLoading) MyBalanceSkeleton()
                    myBalance?.let { balance ->
                        MyBalanceCard(
                            balance = balance,
                            isEveryoneSettled = state.header.isEveryoneSettled,
                            money = money,
                            onSettleClick = { onAction(GroupAction.SETTLE) },
                        )
                    }
                }
            },
            bottom = {
                BalancesTabletopPairsLayout(
                    state = state,
                    money = money,
                    onAction = onAction,
                    onEvent = onEvent,
                )
            },
        )
    }
}

@Composable
private fun BalancesTabletopPairsLayout(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    val contentPadding = PaddingValues(bottom = 24.dp)
    if (state.myDebts.isEmpty() || state.otherDebts.isEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = contentPadding,
        ) {
            balanceItems(
                state = state,
                money = money,
                onAction = onAction,
                onEvent = onEvent,
            )
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding,
        ) {
            simplificationItems(
                state = state,
                onEvent = onEvent,
            )
            withYouItems(
                state = state,
                money = money,
                onEvent = onEvent,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding,
        ) {
            betweenOthersItems(
                state = state,
                money = money,
                onEvent = onEvent,
            )
        }
    }
}

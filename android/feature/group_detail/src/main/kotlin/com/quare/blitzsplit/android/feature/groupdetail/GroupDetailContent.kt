package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace

private const val PREVIEW_TODAY_MILLIS = 1_761_609_600_000L
private const val PREVIEW_HOUR_MILLIS = 3_600_000L
private const val PREVIEW_DAY_MILLIS = 86_400_000L
private val singleColumnMaxWidth = 624.dp
private val twoColumnsMaxWidth = 1100.dp
private val phoneSideColumnWidth = 320.dp
private val tabletSideColumnWidth = 380.dp
private val fabClearance = 96.dp

/*
 * Why: a phone upright stacks the faces, the tabs and the days under the bar, with "Rachar conta" floating; lying
 * down, and on a tablet lying down, the faces and tabs take a column next to the days. A tablet upright centers one
 * column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.isLoaded) {
                GroupDetailTopBar(
                    state = state,
                    money = money,
                    scrollBehavior = scrollBehavior,
                    onEvent = onEvent,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.showsSplitBillButton) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(GroupDetailUiEvent.OnSplitBillClicked) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.group_detail_split_bill)) },
                    expanded = !listState.canScrollBackward,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!state.isLoaded) return@Box
            if (layout.usesTwoColumns) {
                TwoColumnsLayout(
                    state = state,
                    sideWidth = if (layout.isTablet) tabletSideColumnWidth else phoneSideColumnWidth,
                    listState = listState,
                    money = money,
                    dates = dates,
                    onEvent = onEvent,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = fabClearance),
                ) {
                    item(key = "faces") {
                        GroupFacesRow(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    item(key = "tabs") {
                        GroupTabsRow(
                            onEvent = onEvent,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                            ),
                        )
                    }
                    expensesSection(
                        state = state,
                        money = money,
                        dates = dates,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoColumnsLayout(
    state: GroupDetailUiState,
    sideWidth: Dp,
    listState: LazyListState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = twoColumnsMaxWidth)
            .fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .width(sideWidth)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 12.dp,
                    bottom = 24.dp,
                ),
        ) {
            GroupFacesRow(
                state = state,
                onEvent = onEvent,
            )
            GroupTabsRow(
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 12.dp,
                ),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = fabClearance),
        ) {
            expensesSection(
                state = state,
                money = money,
                dates = dates,
                onEvent = onEvent,
            )
        }
    }
}

private fun LazyListScope.expensesSection(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    if (state.isEmpty) {
        item(key = "empty") {
            EmptyExpensesCard(
                onEvent = onEvent,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                ),
            )
        }
    } else {
        expenseDayItems(
            days = state.days,
            money = money,
            dates = dates,
            onEvent = onEvent,
        )
    }
}

private fun createPreviewState(days: List<ExpenseDay>): GroupDetailUiState = GroupDetailUiState(
    groupName = "Viagem Floripa",
    groupInitials = "VF",
    groupColor = AvatarColor.PEACH,
    currency = GroupCurrency.BRL,
    faces = listOf(
        GroupFace("PV", AvatarColor.BLUE),
        GroupFace("AN", AvatarColor.GREEN),
        GroupFace("BR", AvatarColor.PEACH),
        GroupFace("CA", AvatarColor.LILAC),
    ),
    memberCount = 6,
    notJoinedCount = 1,
    totalSpentMinor = if (days.isEmpty()) 0L else 241_860L,
    days = days,
    showsBillDeletedNotice = false,
    isLoaded = true,
)

private fun createPreviewDays(): List<ExpenseDay> = listOf(
    ExpenseDay(
        dayStartMillis = PREVIEW_TODAY_MILLIS,
        relativeDay = RelativeDay.TODAY,
        expenses = listOf(
            ExpenseRow(
                id = "1",
                description = "Mercado",
                spentAt = PREVIEW_TODAY_MILLIS + 17 * PREVIEW_HOUR_MILLIS,
                payerName = "Pierre",
                payerIsMe = true,
                totalMinor = 31_200L,
                myBalanceMinor = 24_960L,
                isPending = true,
            ),
            ExpenseRow(
                id = "2",
                description = "Uber aeroporto",
                spentAt = PREVIEW_TODAY_MILLIS + 9 * PREVIEW_HOUR_MILLIS,
                payerName = "Ana",
                payerIsMe = false,
                totalMinor = 8_760L,
                myBalanceMinor = -2_920L,
                isPending = false,
            ),
        ),
    ),
    ExpenseDay(
        dayStartMillis = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS,
        relativeDay = RelativeDay.OTHER,
        expenses = listOf(
            ExpenseRow(
                id = "3",
                description = "Jantar no Ostradamus",
                spentAt = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS + 21 * PREVIEW_HOUR_MILLIS,
                payerName = "Bruno",
                payerIsMe = false,
                totalMinor = 64_200L,
                myBalanceMinor = -11_800L,
                isPending = false,
            ),
            ExpenseRow(
                id = "4",
                description = "Passeio de barco",
                spentAt = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS + 15 * PREVIEW_HOUR_MILLIS,
                payerName = null,
                payerIsMe = false,
                totalMinor = 72_000L,
                myBalanceMinor = null,
                isPending = false,
            ),
        ),
    ),
)

@DevicePreviews
@Composable
private fun GroupDetailContentPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewState(days = createPreviewDays()),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentEmptyPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewState(days = emptyList()),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.HomeNavigationBar
import com.quare.blitzsplit.android.designsystem.component.HomeNavigationRail
import com.quare.blitzsplit.android.designsystem.component.HomeTab
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.ListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PeopleSection

private val columnMaxWidth = 640.dp
private val sideColumnWidth = 320.dp
private val railWidth = 80.dp

private fun eventOf(tab: HomeTab): GlobalBalancesUiEvent? = when (tab) {
    HomeTab.GROUPS -> GlobalBalancesUiEvent.OnGroupsTabClicked
    HomeTab.BALANCES -> null
    HomeTab.CONTACTS -> GlobalBalancesUiEvent.OnContactsTabClicked
    HomeTab.ACTIVITY -> GlobalBalancesUiEvent.OnActivityTabClicked
}

/*
 * Why: no visible title, since the selected Saldos tab already names it, and no "Rachar conta": the actions are on
 * each person. Upright, one column with "Por grupo" opening in the row; a phone lying down puts the filter on the left
 * and the people on the right, as the app's other screens split. A tablet lying down and an unfolded book list the
 * people beside the one picked, whose groups and actions fill the pane on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalBalancesContent(
    state: GlobalBalancesUiState,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.global_balances_title)
    val profile = stringResource(R.string.global_balances_profile)
    val onTabClick: (HomeTab) -> Unit = { tab -> eventOf(tab)?.let(onEvent) }
    val listDetail = rememberListDetailLayout()
    AdaptiveScreen(modifier = modifier.fillMaxSize().semantics { paneTitle = title }) { layout ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (layout.usesNavigationRail) {
                HomeNavigationRail(
                    selected = HomeTab.BALANCES,
                    onTabClick = onTabClick,
                )
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = {},
                        actions = {
                            IconButton(
                                onClick = { onEvent(GlobalBalancesUiEvent.OnProfileClicked) },
                                modifier = Modifier.semantics { contentDescription = profile },
                            ) {
                                AvatarBadge(
                                    initial = state.avatarInitial,
                                    color = state.avatarColor,
                                    photoUrl = state.photoUrl,
                                )
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!layout.usesNavigationRail) {
                        HomeNavigationBar(
                            selected = HomeTab.BALANCES,
                            onTabClick = onTabClick,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = if (layout.usesNavigationRail) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                } else {
                    WindowInsets.safeDrawing
                },
            ) { padding ->
                if (!state.isLoaded) return@Scaffold
                if (listDetail != null) {
                    BalancesListDetailLayout(
                        state = state,
                        listDetail = listDetail,
                        onEvent = onEvent,
                        modifier = Modifier
                            .padding(padding)
                            .consumeWindowInsets(padding),
                    )
                    return@Scaffold
                }
                BalancesPane(
                    state = state,
                    layout = layout,
                    onEvent = onEvent,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                )
            }
        }
    }
}

@Composable
private fun BalancesPane(
    state: GlobalBalancesUiState,
    layout: DeviceLayout,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layout.usesTwoColumns) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = sideColumnWidth)
                    .padding(top = 4.dp),
            ) {
                FilterHeaderColumn(
                    state = state,
                    onEvent = onEvent,
                )
            }
            PeopleList(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
                header = {},
            )
        }
        return
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        PeopleList(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.widthIn(max = columnMaxWidth),
            header = {
                item(key = "filter") {
                    FilterHeaderColumn(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            },
        )
    }
}

/*
 * Why: the list pane keeps the rail's half (520dp with the rail on a tablet, the half before the hinge on a book), the
 * person picked fills the other; with nothing open the empty state takes the pane instead.
 */
@Composable
private fun BalancesListDetailLayout(
    state: GlobalBalancesUiState,
    listDetail: ListDetailLayout,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedPerson
    Row(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .width(listDetail.listPaneWidth - railWidth)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "filter") {
                FilterHeaderColumn(
                    state = state,
                    onEvent = onEvent,
                )
            }
            pickableSections(
                state = state,
                selectedId = selected?.id,
                onEvent = onEvent,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = listDetail.paneSpacing,
                    end = if (listDetail.isBook) 16.dp else 32.dp,
                    top = 8.dp,
                ),
        ) {
            val emptyState = state.emptyState
            when {
                emptyState != null -> EmptyBalancesSection(
                    emptyState = emptyState,
                    total = state.emptyStateTotal,
                    onAction = { onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked) },
                )

                selected != null -> PersonDetailPane(
                    person = selected,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: the sections of the list beside the detail, each person a row that shows them in the pane.
private fun LazyListScope.pickableSections(
    state: GlobalBalancesUiState,
    selectedId: String?,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    state.sections.forEach { section ->
        item(key = "section-${section.isYouOwe}") { SectionTitle(section) }
        itemsIndexed(section.rows, key = { _, row -> row.id }) { index, row ->
            PersonPickRow(
                row = row,
                isSelected = row.id == selectedId,
                index = index,
                count = section.rows.size,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 1.dp,
                ),
            )
        }
    }
}

// Why: "Você deve · 1" and "Te devem · 4" over their people.
@Composable
private fun SectionTitle(section: PeopleSection) {
    Text(
        text = stringResource(
            if (section.isYouOwe) {
                R.string.global_balances_section_you_owe
            } else {
                R.string.global_balances_section_owes_you
            },
            section.rows.size,
        ),
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 8.dp,
        ),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FilterHeaderColumn(
    state: GlobalBalancesUiState,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.showsOfflineNotice) OfflineChip(modifier = Modifier.padding(horizontal = 16.dp))
        BalancesFilterRow(
            state = state,
            onFilterClick = { onEvent(GlobalBalancesUiEvent.OnFilterChanged(it)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(
                if (state.emptyState == null) R.string.global_balances_hint else R.string.global_balances_hint_empty,
            ),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeopleList(
    state: GlobalBalancesUiState,
    onEvent: (GlobalBalancesUiEvent) -> Unit,
    header: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        header()
        val emptyState = state.emptyState
        if (emptyState != null) {
            item(key = "empty") {
                EmptyBalancesSection(
                    emptyState = emptyState,
                    total = state.emptyStateTotal,
                    onAction = { onEvent(GlobalBalancesUiEvent.OnEmptyActionClicked) },
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "section-${section.isYouOwe}") { SectionTitle(section) }
            itemsIndexed(section.rows, key = { _, row -> row.id }) { index, row ->
                PersonBalanceCard(
                    row = row,
                    index = index,
                    count = section.rows.size,
                    onEvent = onEvent,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 1.dp,
                    ),
                )
            }
        }
    }
}

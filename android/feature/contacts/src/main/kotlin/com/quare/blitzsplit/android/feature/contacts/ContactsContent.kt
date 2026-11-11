package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.HomeBarsScaffold
import com.quare.blitzsplit.android.designsystem.component.HomeTabTitle
import com.quare.blitzsplit.android.designsystem.component.SplitBillFab
import com.quare.blitzsplit.android.designsystem.component.collapsingBarsScroll
import com.quare.blitzsplit.android.designsystem.component.homeTabBarsOf
import com.quare.blitzsplit.android.designsystem.component.rememberCollapsingTopBarScrollBehavior
import com.quare.blitzsplit.android.designsystem.component.splitBillFabClearance
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiEvent
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactsUiState

private val columnMaxWidth = 640.dp
private val tabletColumnMaxWidth = 560.dp
private val listBottomPadding = 24.dp

private fun eventOf(tab: HomeTab): ContactsUiEvent? = when (tab) {
    HomeTab.HOME -> ContactsUiEvent.OnHomeTabClicked
    HomeTab.GROUPS -> ContactsUiEvent.OnGroupsTabClicked
    HomeTab.BALANCES -> ContactsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> null
}

/*
 * Why: the home's Contatos tab, with "Rachar conta" floating on a phone and on top of the rail otherwise, as on every
 * home tab; the other actions are on each profile. Upright, one centered column
 * (560dp on a tablet and a foldable) whose profile and agenda open over it; a phone lying down, a tablet lying down
 * and an unfolded book put the list beside the pane, which draws what `detailPane` holds (the person picked, the
 * agenda or the canvas' shapes) and the empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsContent(
    state: ContactsUiState,
    onEvent: (ContactsUiEvent) -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.contacts_title)
    val onTabClick: (HomeTab) -> Unit = { tab -> eventOf(tab)?.let(onEvent) }
    val paneLayout = rememberContactsPaneLayout()
    val listState = rememberLazyListState()
    val onSplitBillClick = { onEvent(ContactsUiEvent.OnSplitBillClicked) }.takeIf { state.showsSplitBillButton }
    AdaptiveScreen(modifier = modifier.fillMaxSize()) { layout ->
        val columnWidth = if (layout.isTablet) tabletColumnMaxWidth else columnMaxWidth
        // Why: the single list only, whose bottom padding makes room for it; beside a rail or a pane the rail has it.
        val onFabClick = onSplitBillClick?.takeIf { !layout.usesNavigationRail && paneLayout == null }
        // Why: the single list on a phone upright; beside a rail or a pane the bars stay, untouched by a scroll.
        val collapsesBars = !layout.usesNavigationRail && paneLayout == null
        val scrollBehavior = rememberCollapsingTopBarScrollBehavior(
            collapses = collapsesBars,
            canScroll = { listState.canScrollForward || listState.canScrollBackward },
        )
        HomeBarsScaffold(
            bars = homeTabBarsOf(
                usesRail = layout.usesNavigationRail,
                selected = HomeTab.CONTACTS,
                onTabClick = onTabClick,
                onSearchClick = { onEvent(ContactsUiEvent.OnSearchClicked) },
                onSplitBillClick = onSplitBillClick,
                scrollBehavior = scrollBehavior,
            ),
            // Why: the pane's title on the content beside the bars, which TalkBack then still reaches.
            modifier = Modifier.semantics { paneTitle = title },
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .collapsingBarsScroll(scrollBehavior, collapsesBars),
                topBar = {
                    ContactsTopBar(
                        state = state,
                        title = title,
                        scrollBehavior = scrollBehavior.takeIf { collapsesBars },
                        onEvent = onEvent,
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (onFabClick != null) {
                        SplitBillFab(
                            onClick = onFabClick,
                            isExpanded = !listState.canScrollBackward,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { padding ->
                val bodyModifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                if (paneLayout != null) {
                    Row(modifier = bodyModifier.fillMaxSize()) {
                        ContactsLazyList(
                            state = state,
                            isPaneShown = true,
                            onEvent = onEvent,
                            modifier = Modifier
                                .width(paneLayout.listWidth)
                                .fillMaxHeight(),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    start = paneLayout.paneSpacing,
                                    end = paneLayout.paneEndPadding,
                                    top = 8.dp,
                                    bottom = 16.dp,
                                ),
                        ) {
                            if (state.isEmpty && !state.isAgendaPicked) {
                                EmptyPane(onEvent = onEvent)
                            } else {
                                detailPane()
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = bodyModifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        ContactsLazyList(
                            state = state,
                            isPaneShown = false,
                            onEvent = onEvent,
                            listState = listState,
                            bottomPadding = if (onFabClick != null) splitBillFabClearance else listBottomPadding,
                            modifier = Modifier
                                .widthIn(max = columnWidth)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsTopBar(
    state: ContactsUiState,
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
    onEvent: (ContactsUiEvent) -> Unit,
) {
    val profile = stringResource(R.string.contacts_profile)
    TopAppBar(
        title = { HomeTabTitle(title) },
        actions = {
            IconButton(
                onClick = { onEvent(ContactsUiEvent.OnProfileClicked) },
                modifier = Modifier.semantics { contentDescription = profile },
            ) {
                AvatarBadge(
                    initial = state.avatarInitial,
                    color = state.avatarColor,
                    photoUrl = state.photoUrl,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun ContactsLazyList(
    state: ContactsUiState,
    isPaneShown: Boolean,
    onEvent: (ContactsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    bottomPadding: Dp = listBottomPadding,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding),
    ) {
        contactsListItems(
            state = state,
            isPaneShown = isPaneShown,
            onEvent = onEvent,
        )
    }
}

// Why: beside a list with nobody yet, the pane says why and offers the invites.
@Composable
private fun EmptyPane(onEvent: (ContactsUiEvent) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            EmptyContactsContent(
                onEvent = onEvent,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(top = 24.dp),
            )
        }
    }
}

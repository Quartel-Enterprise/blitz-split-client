package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.HomeBars
import com.quare.blitzsplit.android.designsystem.component.homeBarsKindOf
import com.quare.blitzsplit.android.designsystem.component.splitBillIcon
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState

/*
 * Why: the search open in Grupos' place selects Search, the bar's last item, and Grupos then leaves it for the groups;
 * otherwise Grupos is selected and Search opens it.
 */
private fun eventOf(
    tab: HomeTab,
    isSearching: Boolean,
): GroupsUiEvent? = when (tab) {
    HomeTab.HOME -> GroupsUiEvent.OnHomeTabClicked
    HomeTab.GROUPS -> GroupsUiEvent.OnSearchTabLeft.takeIf { isSearching }
    HomeTab.BALANCES -> GroupsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> GroupsUiEvent.OnContactsTabClicked
}

// Why: Grupos' bar or rail, the rail with "Rachar conta" on top.
internal fun groupsBarsOf(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    usesRail: Boolean,
): HomeBars {
    val onSplitBillClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) }.takeIf { state.showsSplitBillButton }
    return HomeBars(
        kind = homeBarsKindOf(usesRail),
        selected = HomeTab.GROUPS.takeUnless { state.isSearching },
        onTabClick = { tab -> eventOf(tab, state.isSearching)?.let(onEvent) },
        onSearchClick = { onEvent(GroupsUiEvent.OnSearchOpened) },
        railHeader = { isShort -> SplitBillRailHeader(isShort, onSplitBillClick) },
    )
}

// Why: Grupos and Início put "Rachar conta" at the top of the rail; Início's first use has none (null).
@Composable
internal fun SplitBillRailHeader(
    isShort: Boolean,
    onSplitBillClick: (() -> Unit)?,
) {
    if (onSplitBillClick == null) return
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = splitBillIcon(),
            contentDescription = stringResource(R.string.groups_split_bill),
        )
    }
    // Why: a phone lying down has the height of the four tabs and Search only beside a small one.
    if (isShort) {
        SmallFloatingActionButton(
            onClick = onSplitBillClick,
            content = icon,
        )
    } else {
        FloatingActionButton(
            onClick = onSplitBillClick,
            modifier = Modifier.padding(top = 16.dp),
            content = icon,
        )
    }
}

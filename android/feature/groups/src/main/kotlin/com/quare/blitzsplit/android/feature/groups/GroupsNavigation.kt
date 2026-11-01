package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.HomeNavigationBar
import com.quare.blitzsplit.android.designsystem.component.HomeNavigationRail
import com.quare.blitzsplit.android.designsystem.component.HomeTab
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

private fun eventOf(tab: HomeTab): GroupsUiEvent? = when (tab) {
    HomeTab.HOME -> GroupsUiEvent.OnHomeTabClicked
    HomeTab.GROUPS -> null
    HomeTab.BALANCES -> GroupsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> GroupsUiEvent.OnContactsTabClicked
    HomeTab.ACTIVITY -> GroupsUiEvent.OnActivityTabClicked
}

@Composable
internal fun GroupsNavigationBar(onEvent: (GroupsUiEvent) -> Unit) {
    HomeNavigationBar(
        selected = HomeTab.GROUPS,
        onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
    )
}

@Composable
internal fun GroupsNavigationRail(onEvent: (GroupsUiEvent) -> Unit) {
    SplitBillNavigationRail(
        selected = HomeTab.GROUPS,
        onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
        onSplitBillClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) },
    )
}

// Why: Grupos and Início put "Rachar conta" at the top of the rail; Início's first use has none (null).
@Composable
internal fun SplitBillNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    onSplitBillClick: (() -> Unit)?,
) {
    HomeNavigationRail(
        selected = selected,
        onTabClick = onTabClick,
        header = {
            if (onSplitBillClick != null) {
                FloatingActionButton(
                    onClick = onSplitBillClick,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.groups_split_bill),
                    )
                }
            }
        },
    )
}

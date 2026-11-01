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
    HomeTab.GROUPS -> null
    HomeTab.BALANCES -> GroupsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> GroupsUiEvent.OnContactsTabClicked
    HomeTab.ACTIVITY -> GroupsUiEvent.OnActivityTabClicked
}

@Composable
internal fun GroupsNavigationBar(
    activityBadgeCount: Int,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    HomeNavigationBar(
        selected = HomeTab.GROUPS,
        onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
        activityBadgeCount = activityBadgeCount,
    )
}

@Composable
internal fun GroupsNavigationRail(
    activityBadgeCount: Int,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    HomeNavigationRail(
        selected = HomeTab.GROUPS,
        onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
        activityBadgeCount = activityBadgeCount,
        header = {
            FloatingActionButton(
                onClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.groups_split_bill),
                )
            }
        },
    )
}

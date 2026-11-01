package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

private enum class GroupsTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val event: GroupsUiEvent?,
) {
    GROUPS(R.string.groups_tab_groups, Icons.Outlined.Groups, null),
    BALANCES(R.string.groups_tab_balances, Icons.Outlined.AccountBalanceWallet, GroupsUiEvent.OnBalancesTabClicked),
    CONTACTS(R.string.groups_tab_contacts, Icons.Outlined.Person, GroupsUiEvent.OnContactsTabClicked),
    ACTIVITY(R.string.groups_tab_activity, Icons.AutoMirrored.Outlined.ReceiptLong, GroupsUiEvent.OnActivityTabClicked),
}

@Composable
internal fun GroupsNavigationBar(onEvent: (GroupsUiEvent) -> Unit) {
    NavigationBar {
        GroupsTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == GroupsTab.GROUPS,
                onClick = { tab.event?.let(onEvent) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
internal fun GroupsNavigationRail(onEvent: (GroupsUiEvent) -> Unit) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
    ) {
        Column(
            modifier = Modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupsTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = tab == GroupsTab.GROUPS,
                    onClick = { tab.event?.let(onEvent) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        }
    }
}

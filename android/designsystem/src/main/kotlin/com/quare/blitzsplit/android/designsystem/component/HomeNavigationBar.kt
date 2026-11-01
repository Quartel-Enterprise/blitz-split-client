package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
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
import com.quare.blitzsplit.android.designsystem.R

// Why: the four tabs of the home, which Grupos and Saldos both draw with their own tab selected.
enum class HomeTab(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    GROUPS(R.string.designsystem_tab_groups, Icons.Outlined.Groups),
    BALANCES(R.string.designsystem_tab_balances, Icons.Outlined.AccountBalanceWallet),
    CONTACTS(R.string.designsystem_tab_contacts, Icons.Outlined.Person),
    ACTIVITY(R.string.designsystem_tab_activity, Icons.AutoMirrored.Outlined.ReceiptLong),
}

@Composable
fun HomeNavigationBar(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
) {
    NavigationBar {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onTabClick(tab) },
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

// Why: a tablet and a phone lying down swap the bottom bar for the rail; Grupos puts "Rachar conta" in its header.
@Composable
fun HomeNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    header: @Composable () -> Unit = {},
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { header() },
    ) {
        Column(
            modifier = Modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = tab == selected,
                    onClick = { if (tab != selected) onTabClick(tab) },
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

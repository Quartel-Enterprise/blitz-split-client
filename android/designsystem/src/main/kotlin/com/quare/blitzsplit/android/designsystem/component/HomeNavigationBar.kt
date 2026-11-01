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
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.HomeTab

// Why: the words and icons of the home's four tabs, which Grupos, Saldos and Contatos draw with their own tab selected.
private val HomeTab.label: Int
    @StringRes get() = when (this) {
        HomeTab.GROUPS -> R.string.designsystem_tab_groups
        HomeTab.BALANCES -> R.string.designsystem_tab_balances
        HomeTab.CONTACTS -> R.string.designsystem_tab_contacts
        HomeTab.ACTIVITY -> R.string.designsystem_tab_activity
    }

private val HomeTab.icon: ImageVector
    get() = when (this) {
        HomeTab.GROUPS -> Icons.Outlined.Groups
        HomeTab.BALANCES -> Icons.Outlined.AccountBalanceWallet
        HomeTab.CONTACTS -> Icons.Outlined.Person
        HomeTab.ACTIVITY -> Icons.AutoMirrored.Outlined.ReceiptLong
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

/*
 * Why: a tablet, a foldable and a phone lying down swap the bottom bar for the rail; Grupos puts "Rachar conta" in its
 * header. A phone lying down has little height, so the rail drops its labels there, as the canvas draws it, and each
 * icon says its tab to TalkBack instead.
 */
@Composable
fun HomeNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    header: @Composable () -> Unit = {},
) {
    val showsLabels = rememberFoldLayout().formFactor != FormFactor.PhoneLandscape
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { header() },
    ) {
        Column(
            modifier = Modifier.padding(top = if (showsLabels) 32.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                val label = stringResource(tab.label)
                NavigationRailItem(
                    selected = tab == selected,
                    onClick = { if (tab != selected) onTabClick(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = if (showsLabels) null else label,
                        )
                    },
                    label = if (showsLabels) {
                        { Text(label) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

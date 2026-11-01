package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
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

// Why: the five tabs of the home, Início first, which Início, Grupos and Saldos draw with their own tab selected.
enum class HomeTab(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.designsystem_tab_home, Icons.Outlined.Home),
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

/*
 * Why: a tablet and a phone lying down swap the bottom bar for the rail; Grupos and Início put "Rachar conta" in its
 * header. A phone lying down has no height for five labelled tabs, so they keep only their icons (TalkBack still reads
 * each name), and a tabletop keeps them in its bottom half, within thumb reach.
 */
@Composable
fun HomeNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    header: @Composable () -> Unit = {},
) {
    val formFactor = rememberFoldLayout().formFactor
    val isShort = formFactor == FormFactor.PhoneLandscape || formFactor == FormFactor.Tabletop
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { header() },
    ) {
        if (formFactor == FormFactor.Tabletop) Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(
                top = if (isShort) 8.dp else 32.dp,
                bottom = if (formFactor == FormFactor.Tabletop) 20.dp else 0.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (isShort) 0.dp else 12.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                val label = stringResource(tab.label)
                NavigationRailItem(
                    selected = tab == selected,
                    onClick = { if (tab != selected) onTabClick(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = label.takeIf { isShort },
                        )
                    },
                    label = if (isShort) null else ({ Text(label) }),
                )
            }
        }
    }
}

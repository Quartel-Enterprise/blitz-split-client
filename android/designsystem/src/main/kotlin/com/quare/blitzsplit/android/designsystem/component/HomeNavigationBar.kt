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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.HomeTab

private const val MAX_BADGE_COUNT = 99

// Why: the words and icons of the home's five tabs, Início first, which each tab draws with its own tab selected.
private val HomeTab.label: Int
    @StringRes get() = when (this) {
        HomeTab.HOME -> R.string.designsystem_tab_home
        HomeTab.GROUPS -> R.string.designsystem_tab_groups
        HomeTab.BALANCES -> R.string.designsystem_tab_balances
        HomeTab.CONTACTS -> R.string.designsystem_tab_contacts
        HomeTab.ACTIVITY -> R.string.designsystem_tab_activity
    }

private val HomeTab.icon: ImageVector
    get() = when (this) {
        HomeTab.HOME -> Icons.Outlined.Home
        HomeTab.GROUPS -> Icons.Outlined.Groups
        HomeTab.BALANCES -> Icons.Outlined.AccountBalanceWallet
        HomeTab.CONTACTS -> Icons.Outlined.Person
        HomeTab.ACTIVITY -> Icons.AutoMirrored.Outlined.ReceiptLong
    }

@Composable
fun HomeNavigationBar(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    activityBadgeCount: Int = 0,
) {
    NavigationBar {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onTabClick(tab) },
                icon = {
                    TabIcon(
                        tab = tab,
                        activityBadgeCount = activityBadgeCount,
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
    activityBadgeCount: Int = 0,
    // Why: beside a screen the selected tab opened (a group's), that tab goes back to it.
    reselects: Boolean = false,
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
                    onClick = { if (reselects || tab != selected) onTabClick(tab) },
                    icon = {
                        TabIcon(
                            tab = tab,
                            activityBadgeCount = activityBadgeCount,
                            contentDescription = label.takeIf { isShort },
                        )
                    },
                    label = if (isShort) null else ({ Text(label) }),
                )
            }
        }
    }
}

/*
 * Why: Atividade's unread count, in the primary color since it asks for a look, never an error; TalkBack reads it
 * with the tab ("Atividade, 3 não lidas"). The rail of a phone lying down names the tab on the icon, having no labels.
 */
@Composable
private fun TabIcon(
    tab: HomeTab,
    activityBadgeCount: Int,
    contentDescription: String?,
) {
    val count = activityBadgeCount.takeIf { tab == HomeTab.ACTIVITY && it > 0 }
    if (count == null) {
        Icon(
            imageVector = tab.icon,
            contentDescription = contentDescription,
        )
        return
    }
    val description = pluralStringResource(R.plurals.designsystem_tab_unread, count, count)
    BadgedBox(
        badge = {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(if (count > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else count.toString())
            }
        },
        modifier = Modifier.semantics { stateDescription = description },
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = contentDescription,
        )
    }
}

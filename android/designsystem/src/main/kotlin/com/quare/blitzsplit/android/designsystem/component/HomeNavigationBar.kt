package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.HomeTab

private val minLabelFontSize = 9.sp
private val labelFontStep = 0.5.sp

// Why: the words and icons of the home's four tabs, Início first, which the one bar draws with the tab in front selected.
private val HomeTab.label: Int
    @StringRes get() = when (this) {
        HomeTab.HOME -> R.string.designsystem_tab_home
        HomeTab.GROUPS -> R.string.designsystem_tab_groups
        HomeTab.BALANCES -> R.string.designsystem_tab_balances
        HomeTab.CONTACTS -> R.string.designsystem_tab_contacts
    }

private val HomeTab.icon: ImageVector
    get() = when (this) {
        HomeTab.HOME -> Icons.Outlined.Home
        HomeTab.GROUPS -> Icons.Outlined.Groups
        HomeTab.BALANCES -> Icons.Outlined.MonetizationOn
        HomeTab.CONTACTS -> Icons.Outlined.Person
    }

/*
 * Why: the home's tabs, then "Buscar" (5.2) as the last item once there is a group to look in (onSearchClick null
 * hides it): last as iOS's search tab, under the thumb, and where the rail keeps it as the phone turns or unfolds
 * (Pierre's call). A null selected is the search in front: Search shows selected and every tab goes somewhere.
 */
@Composable
fun HomeNavigationBar(
    selected: HomeTab?,
    onTabClick: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
) {
    NavigationBar(modifier = modifier) {
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
                label = { NavigationLabelText(stringResource(tab.label)) },
            )
        }
        if (onSearchClick != null || selected == null) {
            NavigationBarItem(
                selected = selected == null,
                onClick = { if (selected != null) onSearchClick?.invoke() },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                label = { NavigationLabelText(stringResource(R.string.designsystem_search)) },
            )
        }
    }
}

/*
 * Why: a tab's name never breaks in the middle ("Balanc/es" at a large font on a 360 dp phone): one line that shrinks
 * to the item's width, and only then ends in an ellipsis.
 */
@Composable
private fun NavigationLabelText(text: String) {
    OneLineShrinkingText(
        text = text,
        style = LocalTextStyle.current,
        minFontSize = minLabelFontSize,
        stepSize = labelFontStep,
    )
}

/*
 * Why: a tablet and a phone lying down swap the bottom bar for the rail; Grupos and Início put "Rachar conta" in its
 * header. A phone lying down has no height for the labelled tabs, so they keep only their icons (TalkBack still reads
 * each name), the header gets whether it is that short to draw itself smaller, and the items scroll should they still
 * not fit, so Search, the last, is never cut off; a tabletop keeps them in its bottom half, within thumb reach.
 */
@Composable
fun HomeNavigationRail(
    selected: HomeTab?,
    onTabClick: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    // Why: "Buscar" as the rail's last item, as on the bar; null hides it, unless the search is in front (selected null).
    onSearchClick: (() -> Unit)? = null,
    // Why: beside a screen the selected tab opened (a group's), that tab goes back to it.
    reselects: Boolean = false,
    header: @Composable (isShort: Boolean) -> Unit = {},
) {
    val formFactor = rememberFoldLayout().formFactor
    val isShort = formFactor == FormFactor.PhoneLandscape || formFactor == FormFactor.Tabletop
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { header(isShort) },
    ) {
        if (formFactor == FormFactor.Tabletop) Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    top = if (isShort) 0.dp else 32.dp,
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
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = label.takeIf { isShort },
                        )
                    },
                    label = if (isShort) null else ({ NavigationLabelText(label) }),
                )
            }
            if (onSearchClick != null || selected == null) {
                val label = stringResource(R.string.designsystem_search)
                NavigationRailItem(
                    selected = selected == null,
                    onClick = { if (selected != null) onSearchClick?.invoke() },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = label.takeIf { isShort },
                        )
                    },
                    label = if (isShort) null else ({ NavigationLabelText(label) }),
                )
            }
        }
    }
}

package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout

/*
 * Why: a home tab (Home, Groups, Balances, Contacts, Activity) names itself only on a wide screen, a tablet or an
 * unfolded foldable, as on every platform (Pierre's call); a phone's bottom bar or rail already names it. The screen
 * keeps its paneTitle for screen readers at every size.
 */
@Composable
fun HomeTabTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    if (!rememberDeviceLayout().isTablet) return
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

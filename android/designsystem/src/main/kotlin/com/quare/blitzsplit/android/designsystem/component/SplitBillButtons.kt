package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R

// Why: room under a home tab's last row for the FAB floating over it, so it never covers that row.
val splitBillFabClearance = 96.dp

/*
 * Why: "Rachar conta" floating over each of the home's four tabs (Início, Grupos, Saldos, Contatos) on a phone, its
 * text going once the content scrolls so it covers less of the rows under it. Beside a rail it is the rail's header.
 */
@Composable
fun SplitBillFab(
    onClick: () -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(splitBillIcon(), contentDescription = null) },
        text = { Text(stringResource(R.string.designsystem_split_bill)) },
        expanded = isExpanded,
        modifier = modifier,
    )
}

// Why: the home's four tabs put "Rachar conta" at the top of the rail; with no group to split in there is none (null).
@Composable
fun SplitBillRailHeader(
    isShort: Boolean,
    onSplitBillClick: (() -> Unit)?,
) {
    if (onSplitBillClick == null) return
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = splitBillIcon(),
            contentDescription = stringResource(R.string.designsystem_split_bill),
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

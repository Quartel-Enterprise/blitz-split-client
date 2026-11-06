package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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

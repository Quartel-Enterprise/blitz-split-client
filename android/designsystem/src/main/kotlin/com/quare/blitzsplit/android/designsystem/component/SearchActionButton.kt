package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R

// Why: the home tabs' magnifier, which opens "Buscar" (5.2) over Início, Saldos, Contatos or Atividade.
@Composable
fun SearchActionButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.designsystem_search),
        )
    }
}

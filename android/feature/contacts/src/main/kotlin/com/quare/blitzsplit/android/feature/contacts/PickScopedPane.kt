package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner

/*
 * Why: the pane beside Contatos draws one person at a time; its ViewModels live while that pick does, so picking
 * twenty people never keeps twenty profiles observing Room. The owner hangs off the entry's, so a rotation keeps them.
 */
@Composable
internal fun PickScopedPane(
    pick: Any,
    content: @Composable () -> Unit,
) {
    key(pick) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides rememberViewModelStoreOwner(),
            content = content,
        )
    }
}

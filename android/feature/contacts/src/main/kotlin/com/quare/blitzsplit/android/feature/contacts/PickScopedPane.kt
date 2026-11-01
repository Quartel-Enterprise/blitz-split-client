package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/*
 * Why: the pane beside Contatos draws one person at a time; its ViewModels live while that pick does, so picking
 * twenty people never keeps twenty profiles observing Room.
 */
@Composable
internal fun PickScopedPane(
    pick: Any,
    content: @Composable () -> Unit,
) {
    key(pick) {
        val owner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
}

package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider

/*
 * Why: the pane beside Contatos draws one pick at a time, and its ViewModels live while that pick shows, so picking
 * twenty people never keeps twenty profiles observing Room. The stores hang off the entry's, so a rotation keeps the
 * pick's. The pick last shown is saved, so the one a rotation leaves without a pane (a tablet turned upright) is
 * cleared once the screen comes back, instead of waiting for a pane that may never show it again.
 */
@Composable
internal fun rememberPaneStores(shownPick: String?): ViewModelStoreProvider {
    val stores = rememberViewModelStoreProvider()
    var lastShownPick by rememberSaveable { mutableStateOf<String?>(null) }
    SideEffect {
        lastShownPick?.takeIf { it != shownPick }?.let(stores::clearKey)
        lastShownPick = shownPick
    }
    return stores
}

@Composable
internal fun PickScopedPane(
    pick: String,
    stores: ViewModelStoreProvider,
    content: @Composable () -> Unit,
) {
    key(pick) {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides rememberViewModelStoreOwner(
                key = pick,
                provider = stores,
            ),
            content = content,
        )
    }
}

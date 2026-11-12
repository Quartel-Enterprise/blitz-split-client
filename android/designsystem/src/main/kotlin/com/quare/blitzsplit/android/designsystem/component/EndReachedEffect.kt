package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// Why: how close to the last row the next page is asked for, so it is read before the list runs out.
private const val END_THRESHOLD = 4

/*
 * Why: a paged list's end asks for the next page (Atividade's, "Todas as despesas"'), once each time it comes near, only
 * while there is more.
 */
@Composable
fun EndReachedEffect(
    listState: LazyListState,
    hasMore: Boolean,
    onEndReached: () -> Unit,
) {
    val currentHasMore by rememberUpdatedState(hasMore)
    val currentOnEndReached by rememberUpdatedState(onEndReached)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - END_THRESHOLD
        }.distinctUntilChanged()
            .filter { it && currentHasMore }
            .collect { currentOnEndReached() }
    }
}

package com.quare.blitzsplit.wear.surface

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/*
 * Why: the Tile and the complications show what the watch's Room holds, so each change of the totals (a sync, "I
 * paid") asks the system to draw them again, while the app's process runs.
 */
object GlanceRefresher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(
        context: Context,
        observePeopleBalances: ObservePeopleBalances,
    ) {
        val appContext = context.applicationContext
        observePeopleBalances()
            .map { it.totals }
            .distinctUntilChanged()
            .drop(1)
            .onEach { refresh(appContext) }
            .launchIn(scope)
    }

    fun refresh(context: Context) {
        TileService.getUpdater(context).requestUpdate(BalanceTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, BalanceComplicationService::class.java))
            .requestUpdateAll()
    }
}

package com.quare.blitzsplit.wear

import android.app.Application
import android.content.Context
import com.quare.blitzsplit.core.sync.di.lastSyncRecordingModule
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import com.quare.blitzsplit.shared.storescreenshots.startBlitzSplitForStoreVideoIfAsked
import com.quare.blitzsplit.wear.push.WearPushSetup
import com.quare.blitzsplit.wear.surface.GlanceRefresher
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

/*
 * Why: the watch is one more device of the account (docs/product/watch.md): the same shared graph as the phone, its
 * own Room database and sync, its own pushes, and the Tile and complication following its balances.
 */
class WearApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LegacyAppLocale.wrapApplication(base))
    }

    override fun onCreate() {
        super.onCreate()
        // Why: a debug build opens on the store screenshots' sample trip when asked, as the phone's does (docs/store-video.md).
        val startedForStoreVideo =
            BuildConfig.DEBUG &&
                startBlitzSplitForStoreVideoIfAsked(this) {
                    androidContext(this@WearApplication)
                    // Why: the watch keeps when it last synced, its "Updated at 14:32".
                    modules(lastSyncRecordingModule)
                }
        if (!startedForStoreVideo) {
            startBlitzSplitKoin(extraModules = listOf(lastSyncRecordingModule)) { androidContext(this@WearApplication) }
        }
        WearPushSetup.start(this, setPushDevice = get())
        GlanceRefresher.start(this, observePeopleBalances = get())
    }
}

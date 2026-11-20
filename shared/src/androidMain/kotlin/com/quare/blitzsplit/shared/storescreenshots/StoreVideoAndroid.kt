package com.quare.blitzsplit.shared.storescreenshots

import android.content.Context
import android.util.Log
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.utils.suspendRunCatching
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.module
import java.io.File

private const val STORE_VIDEO_FLAG = "store-video"
private const val STORE_VIDEO_DATABASE = "store-video.db"
private const val STORE_VIDEO_ASSETS = "store-video-assets"
private const val STORE_VIDEO_READY = "store-video.ready"
private const val STORE_VIDEO_TAG = "StoreVideo"

/*
 * Why: the Play preview video records the real Android app on the sample trip of the store screenshots. A debug build
 * starts on it when files/store-video holds a language tag (written with `adb shell run-as`), in a Room of its own so
 * the developer's data outlives a run. Returns false, starting nothing, when the flag isn't there.
 */
fun startBlitzSplitForStoreVideoIfAsked(
    context: Context,
    configure: KoinApplication.() -> Unit,
): Boolean {
    val flag = File(context.filesDir, STORE_VIDEO_FLAG)
    if (!flag.exists()) return false
    val parts = flag.readText().trim().split(' ')
    val language = parts.first().ifBlank { "en" }
    val isDark = parts.getOrNull(1) == "dark"
    /*
     * Why: the first launch seeds the trip and marks it ready; record.py then starts the app again, which opens the
     * finished trip, so the start route always finds the profile and no scene shows a half-written trip.
     */
    val ready = File(context.filesDir, STORE_VIDEO_READY)
    val isSeeded = ready.exists()
    if (!isSeeded) context.deleteDatabase(STORE_VIDEO_DATABASE)
    val trip = SampleTrips.tripOf(language)
    startBlitzSplitKoin(
        extraModules = listOf(
            storeScreenshotsModuleOf(trip),
            module {
                single<RoomDatabase.Builder<AppDatabase>> {
                    Room
                        .databaseBuilder<AppDatabase>(
                            context = context,
                            name = context.getDatabasePath(STORE_VIDEO_DATABASE).absolutePath,
                        ).setDriver(AndroidSQLiteDriver())
                }
            },
        ),
        configure = configure,
    )
    if (isSeeded) return true
    val assetsDirectory = File(context.filesDir, STORE_VIDEO_ASSETS)
    /*
     * Why: seeding on the main thread makes a slow emulator kill the app for not starting in time; record.py waits
     * for the ready file instead, and a failed seed leaves it unwritten, with the reason in logcat.
     */
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        suspendRunCatching {
            StoreScreenshotsSeed(
                trip = trip,
                assets = StoreScreenshotsAssets { path ->
                    File(assetsDirectory, path).takeIf { it.exists() }?.readBytes()
                },
            ).createTrip(
                language = AppLanguage.fromTag(language),
                theme = if (isDark) ThemePreference.DARK else ThemePreference.LIGHT,
            )
        }.onSuccess { ready.writeText("") }
            .onFailure { error -> Log.e(STORE_VIDEO_TAG, "Seeding the store video's sample trip failed", error) }
    }
    return true
}

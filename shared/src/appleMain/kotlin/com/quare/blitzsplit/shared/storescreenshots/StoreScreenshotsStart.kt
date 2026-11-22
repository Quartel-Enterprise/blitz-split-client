package com.quare.blitzsplit.shared.storescreenshots

import androidx.room3.RoomDatabase
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module

/*
 * Why: the one sequence every Apple screenshot launch follows (the iPhone and the Mac, the Apple Watch's for the
 * README): a fresh Room of its own, the shared code started over it with the sample trip's overrides, then the trip
 * seeded before the first screen reads it. extraModules are what only one app adds.
 */
internal fun startSeededForStoreScreenshots(
    language: String,
    theme: ThemePreference,
    assetsDirectory: String?,
    extraModules: List<Module> = emptyList(),
): SeededTrip {
    deleteStoreScreenshotsDatabase()
    val trip = SampleTrips.tripOf(language)
    startBlitzSplitKoin(
        extraModules = extraModules + listOf(
            storeScreenshotsModuleOf(trip),
            module { single<RoomDatabase.Builder<AppDatabase>> { createStoreScreenshotsDatabaseBuilder() } },
        ),
    )
    /*
     * Why: blocks the launch until the trip is there, off the main thread, so a seed step that hops to Main can't
     * wait on it.
     */
    return runBlocking(Dispatchers.Default) {
        StoreScreenshotsSeed(trip, assetsDirectory?.let(::DirectoryAssets)).createTrip(
            language = AppLanguage.fromTag(language),
            theme = theme,
        )
    }
}

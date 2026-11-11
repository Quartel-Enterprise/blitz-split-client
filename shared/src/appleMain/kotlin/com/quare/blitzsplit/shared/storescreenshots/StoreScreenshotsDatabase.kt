package com.quare.blitzsplit.shared.storescreenshots

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val STORE_SCREENSHOTS_DATABASE = "store-screenshots.db"
private val databaseSuffixes = listOf("", "-wal", "-shm")

/*
 * Why: the screenshots' Room is a file of its own, never blitzsplit.db: on the Mac the debug app the captures launch
 * is the developer's own, whose data (unsynced changes included) must outlive a run.
 */
internal fun createStoreScreenshotsDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> = Room
    .databaseBuilder<AppDatabase>(name = "${getApplicationSupportPath()}/$STORE_SCREENSHOTS_DATABASE")
    .setDriver(BundledSQLiteDriver())

// Why: every screenshot run starts from an empty Room, as a fresh install would, whatever the last run left there.
@OptIn(ExperimentalForeignApi::class)
internal fun deleteStoreScreenshotsDatabase() {
    val fileManager = NSFileManager.defaultManager
    databaseSuffixes.forEach { suffix ->
        val path = "${getApplicationSupportPath()}/$STORE_SCREENSHOTS_DATABASE$suffix"
        if (fileManager.fileExistsAtPath(path)) fileManager.removeItemAtPath(path, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getApplicationSupportPath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(directory?.path)
}

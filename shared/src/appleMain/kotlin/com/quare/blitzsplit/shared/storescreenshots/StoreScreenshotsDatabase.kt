package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.provider.room.db.DATABASE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private val databaseSuffixes = listOf("", "-wal", "-shm")

// Why: every screenshot run starts from an empty Room, as a fresh install would, whatever the last run left there.
@OptIn(ExperimentalForeignApi::class)
internal fun deleteStoreScreenshotsDatabase() {
    val fileManager = NSFileManager.defaultManager
    val directory = fileManager
        .URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path ?: return
    databaseSuffixes.forEach { suffix ->
        val path = "$directory/$DATABASE_NAME$suffix"
        if (fileManager.fileExistsAtPath(path)) fileManager.removeItemAtPath(path, error = null)
    }
}

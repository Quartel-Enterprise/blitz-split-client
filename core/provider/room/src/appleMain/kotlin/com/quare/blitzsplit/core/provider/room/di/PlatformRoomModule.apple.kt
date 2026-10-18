package com.quare.blitzsplit.core.provider.room.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.db.DATABASE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val platformRoomModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room
            .databaseBuilder<AppDatabase>(name = "${getApplicationSupportPath()}/$DATABASE_NAME")
            .setDriver(BundledSQLiteDriver())
    }
}

/*
 * Why: Application Support is private to the app on iOS and inside the sandbox container on macOS,
 * and unlike Documents it is not shown to the person in Files.
 */
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

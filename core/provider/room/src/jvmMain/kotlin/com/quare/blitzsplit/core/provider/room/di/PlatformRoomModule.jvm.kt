package com.quare.blitzsplit.core.provider.room.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

// Why: jvm only runs the unit tests, so its database lives in memory.
internal actual val platformRoomModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver())
    }
}

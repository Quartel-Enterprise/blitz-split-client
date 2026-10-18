package com.quare.blitzsplit.core.provider.room.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.db.DATABASE_NAME
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.Worker

/*
 * Why: the web app creates the SQLite Worker itself (webApp/src/sqlite/worker.js), so Vite bundles
 * it, and hands it to Kotlin when it starts.
 */
internal actual val platformRoomModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room
            .databaseBuilder<AppDatabase>(name = DATABASE_NAME)
            .setDriver(WebWorkerSQLiteDriver(get<Worker>()))
    }
}

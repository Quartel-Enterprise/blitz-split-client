package com.quare.blitzsplit.core.provider.room.di

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.db.DATABASE_NAME
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformRoomModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val context = get<Context>().applicationContext
        Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = context.getDatabasePath(DATABASE_NAME).absolutePath,
            ).setDriver(AndroidSQLiteDriver())
    }
}

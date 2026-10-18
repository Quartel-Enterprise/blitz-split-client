package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.quare.blitzsplit.core.provider.room.dao.AccountDao
import com.quare.blitzsplit.core.provider.room.dao.ProfileDao
import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
@ConstructedBy(DatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun accountDao(): AccountDao
}

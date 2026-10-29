package com.quare.blitzsplit.core.provider.room.di

import androidx.room3.RoomDatabase
import com.quare.blitzsplit.core.provider.room.dao.AccountDao
import com.quare.blitzsplit.core.provider.room.dao.DeviceSettingsDao
import com.quare.blitzsplit.core.provider.room.dao.GroupDao
import com.quare.blitzsplit.core.provider.room.dao.NotificationPreferencesDao
import com.quare.blitzsplit.core.provider.room.dao.PaymentKeyDao
import com.quare.blitzsplit.core.provider.room.dao.ProfileDao
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.utils.ioDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

val roomModule: Module = module {
    includes(platformRoomModule)

    single<AppDatabase> {
        get<RoomDatabase.Builder<AppDatabase>>()
            .setQueryCoroutineContext(ioDispatcher)
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single<ProfileDao> { get<AppDatabase>().profileDao() }
    single<AccountDao> { get<AppDatabase>().accountDao() }
    single<DeviceSettingsDao> { get<AppDatabase>().deviceSettingsDao() }
    single<PaymentKeyDao> { get<AppDatabase>().paymentKeyDao() }
    single<NotificationPreferencesDao> { get<AppDatabase>().notificationPreferencesDao() }
    single<GroupDao> { get<AppDatabase>().groupDao() }
}

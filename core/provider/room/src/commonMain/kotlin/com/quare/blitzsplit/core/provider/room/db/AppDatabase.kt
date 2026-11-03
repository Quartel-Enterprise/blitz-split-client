package com.quare.blitzsplit.core.provider.room.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.quare.blitzsplit.core.provider.room.dao.AccountDao
import com.quare.blitzsplit.core.provider.room.dao.ActivityDao
import com.quare.blitzsplit.core.provider.room.dao.BillAttachmentDao
import com.quare.blitzsplit.core.provider.room.dao.BillCommentDao
import com.quare.blitzsplit.core.provider.room.dao.CategoryTermDao
import com.quare.blitzsplit.core.provider.room.dao.DeviceSettingsDao
import com.quare.blitzsplit.core.provider.room.dao.ExpenseDao
import com.quare.blitzsplit.core.provider.room.dao.GroupCategoryDao
import com.quare.blitzsplit.core.provider.room.dao.GroupDao
import com.quare.blitzsplit.core.provider.room.dao.GroupPinDao
import com.quare.blitzsplit.core.provider.room.dao.HiddenContactDao
import com.quare.blitzsplit.core.provider.room.dao.MemberPaymentKeyDao
import com.quare.blitzsplit.core.provider.room.dao.NotificationPreferencesDao
import com.quare.blitzsplit.core.provider.room.dao.PaymentKeyDao
import com.quare.blitzsplit.core.provider.room.dao.ProfileDao
import com.quare.blitzsplit.core.provider.room.dao.SettlementDao
import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityBackfillEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityEventEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityReadEntity
import com.quare.blitzsplit.core.provider.room.entity.ActivityStateEntity
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import com.quare.blitzsplit.core.provider.room.entity.MemberPaymentKeyEntity
import com.quare.blitzsplit.core.provider.room.entity.MemberPhotoEntity
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
        DeviceSettingsEntity::class,
        PaymentKeyEntity::class,
        NotificationPreferencesEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class,
        ActivityEventEntity::class,
        ActivityReadEntity::class,
        ActivityStateEntity::class,
        ActivityBackfillEntity::class,
        HiddenContactEntity::class,
        GroupPinEntity::class,
        GroupCategoryEntity::class,
        CategoryTermEntity::class,
        MemberPhotoEntity::class,
        MemberPaymentKeyEntity::class,
        BillCommentEntity::class,
        BillAttachmentEntity::class,
    ],
    version = 30,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ),
        AutoMigration(
            from = 2,
            to = 3,
        ),
        AutoMigration(
            from = 3,
            to = 4,
        ),
        AutoMigration(
            from = 4,
            to = 5,
        ),
        AutoMigration(
            from = 5,
            to = 6,
        ),
        AutoMigration(
            from = 6,
            to = 7,
        ),
        AutoMigration(
            from = 7,
            to = 8,
        ),
        AutoMigration(
            from = 8,
            to = 9,
        ),
        AutoMigration(
            from = 9,
            to = 10,
        ),
        AutoMigration(
            from = 10,
            to = 11,
        ),
        AutoMigration(
            from = 11,
            to = 12,
        ),
        AutoMigration(
            from = 12,
            to = 13,
        ),
        AutoMigration(
            from = 13,
            to = 14,
        ),
        AutoMigration(
            from = 14,
            to = 15,
        ),
        AutoMigration(
            from = 15,
            to = 16,
        ),
        AutoMigration(
            from = 16,
            to = 17,
        ),
        AutoMigration(
            from = 17,
            to = 18,
        ),
        AutoMigration(
            from = 18,
            to = 19,
        ),
        AutoMigration(
            from = 19,
            to = 20,
        ),
        AutoMigration(
            from = 20,
            to = 21,
        ),
        AutoMigration(
            from = 21,
            to = 22,
        ),
        AutoMigration(
            from = 22,
            to = 23,
        ),
        AutoMigration(
            from = 23,
            to = 24,
        ),
        AutoMigration(
            from = 24,
            to = 25,
        ),
        AutoMigration(
            from = 25,
            to = 26,
        ),
        AutoMigration(
            from = 26,
            to = 27,
        ),
        AutoMigration(
            from = 27,
            to = 28,
        ),
        AutoMigration(
            from = 28,
            to = 29,
        ),
        AutoMigration(
            from = 29,
            to = 30,
        ),
    ],
)
@ConstructedBy(DatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun accountDao(): AccountDao

    abstract fun deviceSettingsDao(): DeviceSettingsDao

    abstract fun paymentKeyDao(): PaymentKeyDao

    abstract fun notificationPreferencesDao(): NotificationPreferencesDao

    abstract fun groupDao(): GroupDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun settlementDao(): SettlementDao

    abstract fun activityDao(): ActivityDao

    abstract fun hiddenContactDao(): HiddenContactDao

    abstract fun groupPinDao(): GroupPinDao

    abstract fun groupCategoryDao(): GroupCategoryDao

    abstract fun categoryTermDao(): CategoryTermDao

    abstract fun memberPaymentKeyDao(): MemberPaymentKeyDao

    abstract fun billCommentDao(): BillCommentDao

    abstract fun billAttachmentDao(): BillAttachmentDao
}

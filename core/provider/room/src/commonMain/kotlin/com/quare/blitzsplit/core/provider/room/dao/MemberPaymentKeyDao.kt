package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.MemberPaymentKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberPaymentKeyDao {
    @Query("SELECT * FROM member_payment_key WHERE userId = :userId ORDER BY addedAt, id")
    fun observeOf(userId: String): Flow<List<MemberPaymentKeyEntity>>

    @Query("SELECT * FROM member_payment_key")
    fun observeAll(): Flow<List<MemberPaymentKeyEntity>>

    @Query("SELECT * FROM member_payment_key")
    suspend fun getAll(): List<MemberPaymentKeyEntity>

    @Insert
    suspend fun insert(entities: List<MemberPaymentKeyEntity>)

    @Query("DELETE FROM member_payment_key")
    suspend fun deleteAll()

    @Query("DELETE FROM member_payment_key WHERE viewerUserId != :viewerUserId")
    suspend fun deleteOtherViewersKeys(viewerUserId: String)

    // Why: in one write, so a screen never sees the keys gone between the two.
    @Transaction
    suspend fun replaceAll(entities: List<MemberPaymentKeyEntity>) {
        deleteAll()
        insert(entities)
    }
}

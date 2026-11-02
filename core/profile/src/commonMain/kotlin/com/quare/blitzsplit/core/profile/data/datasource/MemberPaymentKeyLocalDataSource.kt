package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.MemberPaymentKeyEntity
import kotlinx.coroutines.flow.Flow

interface MemberPaymentKeyLocalDataSource {
    fun observeOf(userId: String): Flow<List<MemberPaymentKeyEntity>>

    suspend fun replaceAll(entities: List<MemberPaymentKeyEntity>)

    suspend fun clear()
}

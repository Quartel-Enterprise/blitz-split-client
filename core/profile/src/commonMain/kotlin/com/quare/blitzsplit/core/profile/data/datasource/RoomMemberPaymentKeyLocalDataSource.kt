package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.MemberPaymentKeyDao
import com.quare.blitzsplit.core.provider.room.entity.MemberPaymentKeyEntity
import kotlinx.coroutines.flow.Flow

class RoomMemberPaymentKeyLocalDataSource(
    private val dao: MemberPaymentKeyDao,
) : MemberPaymentKeyLocalDataSource {
    override fun observeOf(userId: String): Flow<List<MemberPaymentKeyEntity>> = dao.observeOf(userId)

    override suspend fun replaceAll(entities: List<MemberPaymentKeyEntity>) {
        dao.replaceAll(entities)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

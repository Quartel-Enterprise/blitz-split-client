package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.MemberPaymentKeyDao
import com.quare.blitzsplit.core.provider.room.entity.MemberPaymentKeyEntity
import kotlinx.coroutines.flow.Flow

class RoomMemberPaymentKeyLocalDataSource(
    private val dao: MemberPaymentKeyDao,
) : MemberPaymentKeyLocalDataSource {
    override fun observeOf(userId: String): Flow<List<MemberPaymentKeyEntity>> = dao.observeOf(userId)

    override fun observeAll(): Flow<List<MemberPaymentKeyEntity>> = dao.observeAll()

    override suspend fun getAll(): List<MemberPaymentKeyEntity> = dao.getAll()

    override suspend fun replaceAll(entities: List<MemberPaymentKeyEntity>) {
        dao.replaceAll(entities)
    }

    override suspend fun deleteOtherViewersKeys(viewerUserId: String) {
        dao.deleteOtherViewersKeys(viewerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}

package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.provider.room.dao.AccountDao
import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomAccountLocalDataSource(
    private val dao: AccountDao,
) : AccountLocalDataSource {
    private val writeLock = Mutex()

    private val emptyAccount = AccountEntity(
        id = SINGLE_ROW_ID,
        userId = null,
        provider = null,
        recoveryCode = null,
        recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE.code,
        pendingLinkProvider = null,
    )

    override fun observe(): Flow<AccountEntity> = dao.observe().map { it ?: emptyAccount }

    override suspend fun get(): AccountEntity = dao.get() ?: emptyAccount

    override suspend fun update(transform: (AccountEntity) -> AccountEntity) {
        writeLock.withLock {
            dao.upsert(transform(get()))
        }
    }
}

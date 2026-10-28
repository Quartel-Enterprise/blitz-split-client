package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountLocalDataSource {
    fun observe(): Flow<AccountEntity>

    suspend fun get(): AccountEntity

    suspend fun update(transform: (AccountEntity) -> AccountEntity)

    // Why: back to the state of a fresh install, as the empty row reads.
    suspend fun clear()
}

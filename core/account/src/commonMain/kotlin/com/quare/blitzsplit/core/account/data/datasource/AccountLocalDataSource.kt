package com.quare.blitzsplit.core.account.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountLocalDataSource {
    fun observe(): Flow<AccountEntity>

    suspend fun get(): AccountEntity

    suspend fun update(transform: (AccountEntity) -> AccountEntity)
}

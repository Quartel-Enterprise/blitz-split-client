package com.quare.blitzsplit.core.contacts.data.repository

import com.quare.blitzsplit.core.contacts.data.datasource.GuestMergeRemoteDataSource
import com.quare.blitzsplit.core.contacts.data.mapper.HiddenContactMapper
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching

class GuestMergeRepositoryImpl(
    private val remote: GuestMergeRemoteDataSource,
) : GuestMergeRepository {
    override suspend fun request(memberIds: List<String>): Result<GuestMerge> = suspendRunCatching {
        HiddenContactMapper.toDomain(remote.request(memberIds))
    }

    override suspend fun findPending(memberIds: List<String>): Result<GuestMerge?> = suspendRunCatching {
        remote.fetchPending(memberIds).firstOrNull()?.let(HiddenContactMapper::toDomain)
    }

    override suspend fun cancel(mergeId: String): Result<Unit> = suspendRunCatching {
        remote.cancel(mergeId)
        Unit
    }
}

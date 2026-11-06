package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeDto

interface GuestMergeRemoteDataSource {
    suspend fun request(memberIds: List<String>): GuestMergeDto

    suspend fun fetchPending(memberIds: List<String>): List<GuestMergeDto>

    suspend fun cancel(mergeId: String): Boolean

    // Why: how many of the guest's members moved into the caller's own.
    suspend fun requestClaim(memberIds: List<String>): Int
}

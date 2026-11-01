package com.quare.blitzsplit.core.contacts.domain.repository

import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge

// Why: "Juntar com a conta dele" needs the internet: only the server knows every group's members.
interface GuestMergeRepository {
    // Why: the guest's members, the one whose personal link carries the request first.
    suspend fun request(memberIds: List<String>): Result<GuestMerge>

    // Why: the request waiting for the guest, null when there is none.
    suspend fun findPending(memberIds: List<String>): Result<GuestMerge?>

    suspend fun cancel(mergeId: String): Result<Unit>
}

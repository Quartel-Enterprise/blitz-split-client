package com.quare.blitzsplit.core.contacts.testing

import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository

// In-memory GuestMergeRepository: one pending request at most, every call recorded, failure knobs public.
class FakeGuestMergeRepository(
    var pending: GuestMerge? = null,
) : GuestMergeRepository {
    val requestedMemberIds = mutableListOf<List<String>>()

    // Why: the members each lookup of a pending request asked about.
    val pendingLookups = mutableListOf<List<String>>()
    val cancelledIds = mutableListOf<String>()
    var requestError: Throwable? = null
    var findError: Throwable? = null
    var cancelError: Throwable? = null
    var requestedAt = 0L

    override suspend fun request(memberIds: List<String>): Result<GuestMerge> {
        requestedMemberIds += memberIds
        requestError?.let { return Result.failure(it) }
        val merge = GuestMerge(
            id = "merge-${requestedMemberIds.size}",
            memberIds = memberIds,
            link = "https://blitzsplit.com/g/Code1234/${memberIds.first().take(6)}",
            requestedAt = requestedAt,
        )
        pending = merge
        return Result.success(merge)
    }

    override suspend fun findPending(memberIds: List<String>): Result<GuestMerge?> {
        pendingLookups += memberIds
        findError?.let { return Result.failure(it) }
        return Result.success(pending?.takeIf { merge -> merge.memberIds.any { it in memberIds } })
    }

    override suspend fun cancel(mergeId: String): Result<Unit> {
        cancelledIds += mergeId
        cancelError?.let { return Result.failure(it) }
        if (pending?.id == mergeId) pending = null
        return Result.success(Unit)
    }
}

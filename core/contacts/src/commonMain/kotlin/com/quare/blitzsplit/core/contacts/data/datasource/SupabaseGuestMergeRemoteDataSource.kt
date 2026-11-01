package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.contacts.data.dto.CancelGuestMergeParamsDto
import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeDto
import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeMembersParamsDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabaseGuestMergeRemoteDataSource(
    private val postgrest: Postgrest,
) : GuestMergeRemoteDataSource {
    override suspend fun request(memberIds: List<String>): GuestMergeDto = postgrest
        .rpc(
            function = "request_guest_merge",
            parameters = GuestMergeMembersParamsDto(memberIds),
        ).decodeAs<GuestMergeDto>()

    override suspend fun fetchPending(memberIds: List<String>): List<GuestMergeDto> = postgrest
        .rpc(
            function = "pending_guest_merges",
            parameters = GuestMergeMembersParamsDto(memberIds),
        ).decodeList<GuestMergeDto>()

    override suspend fun cancel(mergeId: String): Boolean = postgrest
        .rpc(
            function = "cancel_guest_merge",
            parameters = CancelGuestMergeParamsDto(mergeId),
        ).decodeAs<Boolean>()
}

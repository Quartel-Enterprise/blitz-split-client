package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.GroupIdParamsDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteCodeParamsDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto
import com.quare.blitzsplit.core.groups.data.dto.RemovedGroupDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc

class SupabaseGroupRemoteDataSource(
    private val postgrest: Postgrest,
) : GroupRemoteDataSource {
    override suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto> = postgrest
        .from(GROUPS)
        .upsert(groups) { select() }
        .decodeList<GroupDto>()

    override suspend fun upsertMembers(members: List<GroupMemberDto>): List<GroupMemberDto> = postgrest
        .from(MEMBERS)
        .upsert(members) { select() }
        .decodeList<GroupMemberDto>()

    // Why: a few ids per request, so the filter in the URL stays short.
    override suspend fun deleteMembers(ids: List<String>) {
        ids.chunked(IDS_PER_DELETE).forEach { chunk ->
            postgrest.from(MEMBERS).delete {
                filter { isIn("id", chunk) }
            }
        }
    }

    override suspend fun fetchGroups(): List<GroupDto> = fetchAll(GROUPS)

    override suspend fun fetchMembers(): List<GroupMemberDto> = fetchAll(MEMBERS)

    override suspend fun fetchInvite(code: String): InviteDto? = postgrest
        .rpc(
            function = GET_INVITE,
            parameters = InviteCodeParamsDto(code),
        ).decodeAs<InviteDto?>()

    override suspend fun fetchPersonalInvite(params: PersonalInviteParamsDto): InviteDto? = postgrest
        .rpc(
            function = GET_INVITE,
            parameters = params,
        ).decodeAs<InviteDto?>()

    override suspend fun joinGroup(params: JoinGroupParamsDto): JoinGroupResultDto = postgrest
        .rpc(
            function = JOIN_GROUP,
            parameters = params,
        ).decodeAs<JoinGroupResultDto>()

    override suspend fun requestMemberRelease(params: ReleaseMemberParamsDto): GroupMemberDto? = postgrest
        .rpc(
            function = RELEASE_MEMBER,
            parameters = params,
        ).decodeAs<GroupMemberDto?>()

    override suspend fun requestLeaveGroup(groupId: String): GroupExitResultDto = postgrest
        .rpc(
            function = LEAVE_GROUP,
            parameters = GroupIdParamsDto(groupId),
        ).decodeAs<GroupExitResultDto>()

    override suspend fun deleteGroup(groupId: String): GroupExitResultDto = postgrest
        .rpc(
            function = DELETE_GROUP,
            parameters = GroupIdParamsDto(groupId),
        ).decodeAs<GroupExitResultDto>()

    // Why: a few ids per request, so the filter in the URL stays short; row level security keeps it to this user's.
    override suspend fun fetchRemovedGroups(groupIds: List<String>): List<RemovedGroupDto> = groupIds
        .chunked(IDS_PER_DELETE)
        .flatMap { chunk ->
            postgrest
                .from(REMOVED_GROUPS)
                .select {
                    filter { isIn("group_id", chunk) }
                }.decodeList<RemovedGroupDto>()
        }

    /*
     * Why: page by page, since the server answers at most max_rows per request, until a page comes back empty: a
     * short page proves nothing if max_rows is below the page asked for. A row missing from the answer would read as
     * removed on the server and leave the device.
     */
    private suspend inline fun <reified T : Any> fetchAll(table: String): List<T> {
        val rows = mutableListOf<T>()
        do {
            val from = rows.size.toLong()
            val page = postgrest
                .from(table)
                .select {
                    order(
                        column = "id",
                        order = Order.ASCENDING,
                    )
                    range(
                        from = from,
                        to = from + PAGE_SIZE - 1,
                    )
                }.decodeList<T>()
            rows += page
        } while (page.isNotEmpty())
        return rows
    }

    private companion object {
        const val GROUPS = "groups"
        const val MEMBERS = "group_members"
        const val GET_INVITE = "get_invite"
        const val JOIN_GROUP = "join_group"
        const val RELEASE_MEMBER = "release_member"

        const val LEAVE_GROUP = "leave_group"
        const val DELETE_GROUP = "delete_group"
        const val REMOVED_GROUPS = "removed_groups"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L

        const val IDS_PER_DELETE = 100
    }
}

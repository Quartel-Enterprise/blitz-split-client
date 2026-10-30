package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteCodeParamsDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
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

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L

        const val IDS_PER_DELETE = 100
    }
}

package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.FirstDownloadGroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.GroupIdParamsDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteCodeParamsDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.MemberPhotoDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto
import com.quare.blitzsplit.core.groups.data.dto.RemovedGroupDto
import com.quare.blitzsplit.core.groups.data.dto.RotateInviteCodeParamsDto
import com.quare.blitzsplit.core.groups.domain.model.InviteCodeTakenException
import com.quare.blitzsplit.core.groups.domain.model.NotGroupCreatorException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc

class SupabaseGroupRemoteDataSource(
    private val postgrest: Postgrest,
) : GroupRemoteDataSource {
    override suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto> = try {
        postgrest
            .from(GROUPS)
            .upsert(groups) { select() }
            .decodeList<GroupDto>()
    } catch (exception: PostgrestRestException) {
        if (exception.isInviteCodeTaken()) throw InviteCodeTakenException()
        throw exception
    }

    /*
     * Why: set_group_invite_code refuses a taken code by name; two groups going up at once with the same code meet the
     * unique constraint instead.
     */
    private fun PostgrestRestException.isInviteCodeTaken(): Boolean = code == UNIQUE_VIOLATION &&
        (INVITE_CODE_TAKEN in error || INVITE_CODE_UNIQUE in error)

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

    override suspend fun fetchFirstDownloadGroupIds(): List<String> = postgrest
        .rpc(FIRST_DOWNLOAD_GROUPS)
        .decodeList<FirstDownloadGroupDto>()
        .map { it.groupId }

    override suspend fun fetchGroup(groupId: String): GroupDto? = postgrest
        .from(GROUPS)
        .select { filter { eq("id", groupId) } }
        .decodeSingleOrNull<GroupDto>()

    // Why: a group's members fit in one page, far under the API's max_rows.
    override suspend fun fetchMembers(groupId: String): List<GroupMemberDto> = postgrest
        .from(MEMBERS)
        .select { filter { eq("group_id", groupId) } }
        .decodeList<GroupMemberDto>()

    // Why: only the id and the photo, never the rest of someone else's profile.
    override suspend fun fetchMemberPhotos(): List<MemberPhotoDto> = fetchAll(
        table = PROFILES,
        columns = Columns.list("id", "avatar_url"),
    )

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

    override suspend fun rotateInviteCode(groupId: String): String = try {
        postgrest
            .rpc(
                function = ROTATE_INVITE_CODE,
                parameters = RotateInviteCodeParamsDto(groupId),
            ).decodeAs<String>()
    } catch (exception: PostgrestRestException) {
        if (exception.code == INSUFFICIENT_PRIVILEGE) throw NotGroupCreatorException()
        throw exception
    }

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
    private suspend inline fun <reified T : Any> fetchAll(
        table: String,
        columns: Columns = Columns.ALL,
    ): List<T> {
        val rows = mutableListOf<T>()
        do {
            val from = rows.size.toLong()
            val page = postgrest
                .from(table)
                .select(columns) {
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
        const val PROFILES = "profiles"
        const val GET_INVITE = "get_invite"
        const val JOIN_GROUP = "join_group"
        const val ROTATE_INVITE_CODE = "rotate_invite_code"
        const val RELEASE_MEMBER = "release_member"

        // Why: the Postgres code rotate_invite_code raises for anyone but the group's creator.
        const val INSUFFICIENT_PRIVILEGE = "42501"
        const val UNIQUE_VIOLATION = "23505"
        const val INVITE_CODE_TAKEN = "invite_code_taken"
        const val INVITE_CODE_UNIQUE = "groups_invite_code_key"
        const val LEAVE_GROUP = "leave_group"
        const val DELETE_GROUP = "delete_group"
        const val REMOVED_GROUPS = "removed_groups"
        const val FIRST_DOWNLOAD_GROUPS = "first_download_groups"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L

        const val IDS_PER_DELETE = 100
    }
}

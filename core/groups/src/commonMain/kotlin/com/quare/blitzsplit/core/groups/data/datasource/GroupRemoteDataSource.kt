package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto

interface GroupRemoteDataSource {
    // Why: the rows as the server kept them, with the invite code it made for a new group.
    suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto>

    // Why: the rows as the server kept them, with the invite code it made for a new member.
    suspend fun upsertMembers(members: List<GroupMemberDto>): List<GroupMemberDto>

    // Why: the server only deletes members nobody joined as; it leaves the others where they are.
    suspend fun deleteMembers(ids: List<String>)

    // Why: every group the user may see, which the server's row level security decides.
    suspend fun fetchGroups(): List<GroupDto>

    suspend fun fetchMembers(): List<GroupMemberDto>

    // Why: the group behind an invite code, which nobody outside it may read otherwise; null for a code no group has.
    suspend fun fetchInvite(code: String): InviteDto?

    // Why: the same for a personal link, with the member it joins as and whoever shared it.
    suspend fun fetchPersonalInvite(params: PersonalInviteParamsDto): InviteDto?

    suspend fun joinGroup(params: JoinGroupParamsDto): JoinGroupResultDto

    // Why: the group's new invite code; anyone but the group's creator gets NotGroupCreatorException.
    suspend fun rotateInviteCode(groupId: String): String

    // Why: the member as the server keeps it once released; null for a member no longer on the server.
    suspend fun requestMemberRelease(params: ReleaseMemberParamsDto): GroupMemberDto?
}

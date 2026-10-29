package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto

interface GroupRemoteDataSource {
    // Why: the rows as the server kept them, with the invite code it made for a new group.
    suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto>

    suspend fun upsertMembers(members: List<GroupMemberDto>)

    // Why: the server only deletes members nobody joined as; it leaves the others where they are.
    suspend fun deleteMembers(ids: List<String>)

    // Why: every group the user may see, which the server's row level security decides.
    suspend fun fetchGroups(): List<GroupDto>

    suspend fun fetchMembers(): List<GroupMemberDto>
}

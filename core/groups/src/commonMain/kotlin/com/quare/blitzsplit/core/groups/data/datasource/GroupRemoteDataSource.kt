package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto

interface GroupRemoteDataSource {
    suspend fun upsertGroups(groups: List<GroupDto>)

    suspend fun upsertMembers(members: List<GroupMemberDto>)

    // Why: every group the user may see, which the server's row level security decides.
    suspend fun fetchGroups(): List<GroupDto>

    suspend fun fetchMembers(): List<GroupMemberDto>
}

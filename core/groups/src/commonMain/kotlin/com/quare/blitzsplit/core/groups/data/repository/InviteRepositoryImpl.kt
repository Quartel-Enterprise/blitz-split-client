package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.mapper.InviteMapper
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.utils.suspendRunCatching

class InviteRepositoryImpl(
    private val remote: GroupRemoteDataSource,
    private val mapper: InviteMapper,
) : InviteRepository {
    override suspend fun fetchInvite(code: String): Result<Invite?> = suspendRunCatching {
        remote.fetchInvite(code)?.let { dto -> mapper.toDomain(code, dto) }
    }

    override suspend fun joinGroup(
        code: String,
        choice: JoinChoice,
        avatarColor: AvatarColor,
    ): Result<JoinOutcome> = suspendRunCatching {
        val params = JoinGroupParamsDto(
            code = code,
            avatarColor = avatarColor.code,
            memberId = (choice as? JoinChoice.AsMember)?.memberId,
            newName = (choice as? JoinChoice.AsNewMember)?.name,
        )
        mapper.toDomain(remote.joinGroup(params))
    }
}

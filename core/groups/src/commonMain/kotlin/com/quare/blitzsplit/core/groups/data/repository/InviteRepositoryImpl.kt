package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
import com.quare.blitzsplit.core.groups.data.mapper.InviteMapper
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.utils.suspendRunCatching

class InviteRepositoryImpl(
    private val remote: GroupRemoteDataSource,
    private val mapper: InviteMapper,
) : InviteRepository {
    // Why: the group's link asks with its code alone, so the server reads it as before personal links existed.
    override suspend fun fetchInvite(target: InviteTarget): Result<Invite?> = suspendRunCatching {
        val dto = if (target.memberCode == null) {
            remote.fetchInvite(target.code)
        } else {
            remote.fetchPersonalInvite(
                PersonalInviteParamsDto(
                    code = target.code,
                    memberCode = target.memberCode,
                    inviterCode = target.inviterCode,
                ),
            )
        }
        dto?.let { mapper.toDomain(target.code, it) }
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

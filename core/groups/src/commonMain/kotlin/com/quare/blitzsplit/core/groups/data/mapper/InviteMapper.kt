package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome

interface InviteMapper {
    fun toDomain(
        code: String,
        dto: InviteDto,
    ): Invite

    fun toDomain(dto: JoinGroupResultDto): JoinOutcome
}

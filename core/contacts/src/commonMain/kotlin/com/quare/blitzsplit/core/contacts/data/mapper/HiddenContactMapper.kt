package com.quare.blitzsplit.core.contacts.data.mapper

import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeDto
import com.quare.blitzsplit.core.contacts.data.dto.HiddenContactDto
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity

object HiddenContactMapper {
    fun toDomain(entity: HiddenContactEntity): HiddenContact = HiddenContact(
        personKey = entity.personKey,
        isHidden = entity.isHidden,
        hiddenAt = entity.hiddenAt,
    )

    fun toDto(
        entity: HiddenContactEntity,
        userId: String,
    ): HiddenContactDto = HiddenContactDto(
        userId = userId,
        personKey = entity.personKey,
        isHidden = entity.isHidden,
        hiddenAt = entity.hiddenAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(
        dto: HiddenContactDto,
        ownerUserId: String,
    ): HiddenContactEntity = HiddenContactEntity(
        personKey = dto.personKey,
        isHidden = dto.isHidden,
        hiddenAt = dto.hiddenAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )

    // Why: the guest's personal link, which joins straight in as them and accepts the request.
    fun toDomain(dto: GuestMergeDto): GuestMerge = GuestMerge(
        id = dto.id,
        memberIds = dto.memberIds,
        link = InviteLink.personalOf(
            inviteCode = dto.inviteCode,
            memberCode = dto.memberInviteCode,
            inviterCode = null,
        ),
        requestedAt = dto.requestedAt,
    )
}

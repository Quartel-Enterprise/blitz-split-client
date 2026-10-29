package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

fun interface CreateGroup {
    // Why: the new group's id. The members go in the order given, this device's user first.
    suspend operator fun invoke(
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
        members: List<NewGroupMember>,
    ): Result<String>
}

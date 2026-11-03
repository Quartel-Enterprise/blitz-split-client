package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto

fun interface CreateGroup {
    // Why: the new group's id. The members go in the order given, this device's user first.
    suspend operator fun invoke(
        name: String,
        type: GroupType,
        color: AvatarColor,
        shape: GroupShape,
        // Why: a photo picked in "Novo grupo", which goes up with the group's first sync.
        photo: ProfilePhoto.Pending?,
        currency: GroupCurrency,
        members: List<NewGroupMember>,
    ): Result<String>
}

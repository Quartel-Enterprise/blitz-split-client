package com.quare.blitzsplit.feature.profile.presentation.mapper

import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInvite
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInviteStatus

// Why: the phone in use wins over a dismissal, since the row then says so whatever closed the invite.
internal fun toProfileAppInvite(invite: AppInvite): ProfileAppInvite = ProfileAppInvite(
    status = when {
        invite.phoneUse != null -> ProfileAppInviteStatus.IN_USE
        invite.isDismissed -> ProfileAppInviteStatus.DISMISSED
        else -> ProfileAppInviteStatus.ACTIVE
    },
    link = invite.link,
    usesIphone = invite.phoneUse?.platforms?.contains(ClientPlatform.IOS) == true,
)

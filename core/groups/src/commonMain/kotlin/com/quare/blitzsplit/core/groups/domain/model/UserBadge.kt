package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.model.toImageUrl

/*
 * Why: the device's user as the home's frame draws them (the avatar, the name and the account under it): the name
 * trimmed, its first letter, the default color before there is a profile, and the provider only once connected.
 */
data class UserBadge(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    val photoUrl: String?,
    val accountProvider: AccountProvider?,
) {
    companion object {
        fun of(
            profile: UserProfile?,
            status: AccountStatus,
        ): UserBadge {
            val name = profile?.displayName?.trim().orEmpty()
            return UserBadge(
                displayName = name,
                avatarInitial = Initials.firstOf(name),
                avatarColor = profile?.avatarColor ?: AvatarColor.default,
                photoUrl = profile?.photo?.toImageUrl(),
                accountProvider = (status as? AccountStatus.Connected)?.provider,
            )
        }
    }
}

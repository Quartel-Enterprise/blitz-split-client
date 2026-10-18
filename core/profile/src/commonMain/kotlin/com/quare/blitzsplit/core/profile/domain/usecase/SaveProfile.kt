package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.UserProfile

fun interface SaveProfile {
    suspend operator fun invoke(profile: UserProfile)
}

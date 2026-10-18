package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile

class SaveProfileUseCase(
    private val repository: ProfileRepository,
) : SaveProfile {
    override suspend fun invoke(profile: UserProfile) {
        repository.saveProfile(profile.copy(displayName = profile.displayName.trim()))
    }
}

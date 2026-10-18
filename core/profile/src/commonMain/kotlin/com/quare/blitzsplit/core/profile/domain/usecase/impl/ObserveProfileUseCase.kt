package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import kotlinx.coroutines.flow.Flow

class ObserveProfileUseCase(
    private val repository: ProfileRepository,
) : ObserveProfile {
    override fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}

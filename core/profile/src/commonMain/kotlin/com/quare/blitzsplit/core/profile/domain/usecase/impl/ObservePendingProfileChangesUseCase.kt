package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingProfileChanges
import kotlinx.coroutines.flow.Flow

class ObservePendingProfileChangesUseCase(
    private val repository: ProfileRepository,
) : ObservePendingProfileChanges {
    override fun invoke(): Flow<Boolean> = repository.observeHasPendingChanges()
}

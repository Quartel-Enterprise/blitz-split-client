package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfileShareLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveProfileShareLinkUseCase(
    private val repository: ProfileRepository,
) : ObserveProfileShareLink {
    override fun invoke(): Flow<String?> = repository.observeShareCode().map { code -> code?.let(ProfileLink::of) }
}

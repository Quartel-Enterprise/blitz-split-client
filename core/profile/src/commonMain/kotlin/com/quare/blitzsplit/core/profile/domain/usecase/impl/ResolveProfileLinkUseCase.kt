package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileLinkRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ResolveProfileLink

class ResolveProfileLinkUseCase(
    private val repository: ProfileLinkRepository,
) : ResolveProfileLink {
    override suspend fun invoke(code: String): Result<SharedProfile?> = repository.resolve(code)
}

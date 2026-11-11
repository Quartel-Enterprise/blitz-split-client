package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.profile.domain.model.SharedProfile

fun interface ResolveProfileLink {
    suspend operator fun invoke(code: String): Result<SharedProfile?>
}

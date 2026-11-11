package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.profile.domain.model.SharedProfile

fun interface ProfileLinkRepository {
    // Why: asks the server, so it fails offline; a success of null is a code no profile has.
    suspend fun resolve(code: String): Result<SharedProfile?>
}

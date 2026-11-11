package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.SharedProfileDto

fun interface ProfileLinkRemoteDataSource {
    // Why: null for a code no profile has.
    suspend fun fetchSharedProfile(code: String): SharedProfileDto?
}

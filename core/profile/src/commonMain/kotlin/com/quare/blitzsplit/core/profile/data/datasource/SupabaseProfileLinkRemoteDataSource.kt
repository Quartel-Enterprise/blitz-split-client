package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.ShareCodeParamsDto
import com.quare.blitzsplit.core.profile.data.dto.SharedProfileDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabaseProfileLinkRemoteDataSource(
    private val postgrest: Postgrest,
) : ProfileLinkRemoteDataSource {
    override suspend fun fetchSharedProfile(code: String): SharedProfileDto? = postgrest
        .rpc(
            function = GET_PROFILE_BY_SHARE_CODE,
            parameters = ShareCodeParamsDto(code),
        ).decodeList<SharedProfileDto>()
        .firstOrNull()

    private companion object {
        const val GET_PROFILE_BY_SHARE_CODE = "get_profile_by_share_code"
    }
}

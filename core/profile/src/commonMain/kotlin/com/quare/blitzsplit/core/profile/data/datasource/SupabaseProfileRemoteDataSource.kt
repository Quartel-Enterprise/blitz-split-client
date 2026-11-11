package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.profile.domain.model.ProfileCodeTakenException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

class SupabaseProfileRemoteDataSource(
    private val postgrest: Postgrest,
) : ProfileRemoteDataSource {
    override suspend fun upsert(profile: ProfileDto) {
        try {
            postgrest.from(TABLE).upsert(profile)
        } catch (exception: PostgrestRestException) {
            if (exception.isShareCodeTaken()) throw ProfileCodeTakenException()
            throw exception
        }
    }

    override suspend fun fetch(userId: String): ProfileDto? = postgrest
        .from(TABLE)
        .select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileDto>()

    /*
     * Why: set_profile_share_code refuses a taken code by name, whatever its SQLSTATE; two profiles going up at once
     * with the same code meet the unique constraint instead.
     */
    private fun PostgrestRestException.isShareCodeTaken(): Boolean = SHARE_CODE_TAKEN in error ||
        (code == UNIQUE_VIOLATION && SHARE_CODE_UNIQUE in error)

    private companion object {
        const val TABLE = "profiles"
        const val UNIQUE_VIOLATION = "23505"
        const val SHARE_CODE_TAKEN = "profile_code_taken"
        const val SHARE_CODE_UNIQUE = "profiles_share_code_key"
    }
}

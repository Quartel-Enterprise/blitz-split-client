package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.profile.domain.model.ProfileCodeTakenException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class SupabaseProfileRemoteDataSource(
    private val postgrest: Postgrest,
) : ProfileRemoteDataSource {
    override suspend fun upsert(profile: ProfileDto) {
        try {
            postgrest.from(TABLE).upsert(profile)
        } catch (exception: PostgrestRestException) {
            when {
                exception.isShareCodeTaken() -> throw ProfileCodeTakenException()
                exception.isMissingShareCodeColumn() -> postgrest.from(TABLE).upsert(profile.toJsonWithoutShareCode())
                else -> throw exception
            }
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

    /*
     * Why: an API from before profile links (blitz-split-api#59 not deployed yet) refuses the unknown column, which
     * would fail every sync; the profile then goes up without it, and the server's code comes down once it has one.
     */
    private fun PostgrestRestException.isMissingShareCodeColumn(): Boolean =
        code == MISSING_COLUMN && SHARE_CODE_COLUMN in error

    private fun ProfileDto.toJsonWithoutShareCode(): JsonObject =
        JsonObject(Json.encodeToJsonElement(this).jsonObject - SHARE_CODE_COLUMN)

    private companion object {
        const val TABLE = "profiles"
        const val UNIQUE_VIOLATION = "23505"
        const val SHARE_CODE_TAKEN = "profile_code_taken"
        const val SHARE_CODE_UNIQUE = "profiles_share_code_key"

        // Why: PostgREST's code for a column its schema cache doesn't have.
        const val MISSING_COLUMN = "PGRST204"
        const val SHARE_CODE_COLUMN = "share_code"
    }
}

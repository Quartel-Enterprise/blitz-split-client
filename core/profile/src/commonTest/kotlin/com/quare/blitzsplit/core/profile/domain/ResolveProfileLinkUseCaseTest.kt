package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ResolveProfileLinkUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveProfileLinkUseCaseTest {
    private val larissa = SharedProfile(
        displayName = "Larissa",
        avatarColor = AvatarColor.PEACH,
        photoUrl = null,
        userId = "user-2",
    )
    private val resolvedCodes = mutableListOf<String>()
    private val useCase = ResolveProfileLinkUseCase { code ->
        resolvedCodes += code
        Result.success(larissa)
    }

    @Test
    fun `GIVEN a profile code WHEN resolving THEN gives the profile the repository finds for it`() = runTest {
        // Given
        val code = "7Hk2pQ8xZa"

        // When
        val result = useCase(code)

        // Then
        assertEquals(larissa, result.getOrNull())
        assertEquals(listOf(code), resolvedCodes)
    }
}

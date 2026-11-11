package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLinkRemoteDataSource
import com.quare.blitzsplit.core.profile.data.dto.SharedProfileDto
import com.quare.blitzsplit.core.profile.data.repository.ProfileLinkRepositoryImpl
import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileLinkRepositoryImplTest {
    private lateinit var remote: FakeProfileLinkRemoteDataSource
    private lateinit var repository: ProfileLinkRepositoryImpl

    @Test
    fun `GIVEN a code of a profile WHEN resolving THEN gives its name color photo and account`() = runTest {
        // Given
        prepareScenario(sharedProfile = larissa)

        // When
        val result = repository.resolve(CODE)

        // Then
        assertEquals(
            SharedProfile(
                displayName = "Larissa",
                avatarColor = AvatarColor.PEACH,
                photoUrl = PHOTO_URL,
                userId = "user-2",
            ),
            result.getOrNull(),
        )
        assertEquals(listOf(CODE), remote.fetchedCodes)
    }

    @Test
    fun `GIVEN a profile whose color this version does not know WHEN resolving THEN shows the default color`() =
        runTest {
            // Given
            prepareScenario(sharedProfile = larissa.copy(avatarColor = "ultraviolet"))

            // When
            val result = repository.resolve(CODE)

            // Then
            assertEquals(AvatarColor.default, result.getOrNull()?.avatarColor)
        }

    @Test
    fun `GIVEN the server withholds the account of the profile WHEN resolving THEN gives the profile without it`() =
        runTest {
            // Given
            prepareScenario(sharedProfile = larissa.copy(userId = null))

            // When
            val result = repository.resolve(CODE)

            // Then
            val profile = result.getOrNull()
            assertEquals("Larissa", profile?.displayName)
            assertNull(profile?.userId)
        }

    @Test
    fun `GIVEN a code no profile has WHEN resolving THEN gives no profile`() = runTest {
        // Given
        prepareScenario(sharedProfile = null)

        // When
        val result = repository.resolve(CODE)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN the server cannot be reached WHEN resolving THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = repository.resolve(CODE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a code too short WHEN resolving THEN gives no profile without asking the server`() = runTest {
        // Given
        prepareScenario(sharedProfile = larissa)

        // When
        val result = repository.resolve("7Hk2pQ8xZ")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(remote.fetchedCodes.isEmpty())
    }

    @Test
    fun `GIVEN a code too long WHEN resolving THEN gives no profile without asking the server`() = runTest {
        // Given
        prepareScenario(sharedProfile = larissa)

        // When
        val result = repository.resolve("7Hk2pQ8xZab")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(remote.fetchedCodes.isEmpty())
    }

    @Test
    fun `GIVEN a code with a symbol WHEN resolving THEN gives no profile without asking the server`() = runTest {
        // Given
        prepareScenario(sharedProfile = larissa)

        // When
        val result = repository.resolve("7Hk2'Q8xZa")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(remote.fetchedCodes.isEmpty())
    }

    private val larissa = SharedProfileDto(
        displayName = "Larissa",
        avatarColor = "peach",
        avatarUrl = PHOTO_URL,
        userId = "user-2",
    )

    private fun prepareScenario(
        sharedProfile: SharedProfileDto? = null,
        error: Throwable? = null,
    ) {
        remote = FakeProfileLinkRemoteDataSource(
            sharedProfile = sharedProfile,
            error = error,
        )
        repository = ProfileLinkRepositoryImpl(remote)
    }

    private companion object {
        const val CODE = "7Hk2pQ8xZa"
        const val PHOTO_URL = "https://storage/avatars/user-2/avatar.jpg?v=3"
    }
}

private class FakeProfileLinkRemoteDataSource(
    private val sharedProfile: SharedProfileDto?,
    private val error: Throwable?,
) : ProfileLinkRemoteDataSource {
    val fetchedCodes = mutableListOf<String>()

    override suspend fun fetchSharedProfile(code: String): SharedProfileDto? {
        fetchedCodes += code
        error?.let { throw it }
        return sharedProfile
    }
}

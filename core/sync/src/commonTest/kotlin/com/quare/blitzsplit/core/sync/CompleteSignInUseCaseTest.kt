package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CompleteSignInUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteSignInUseCaseTest {
    private val googleUser = SessionUser(
        userId = "google-user",
        socialProvider = AccountProvider.GOOGLE,
        displayName = "Pierre",
        photoUrl = null,
        email = null,
    )
    private val pierre = UserProfile("Pierre", AvatarColor.GREEN, null)
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var useCase: CompleteSignInUseCase

    @Test
    fun `GIVEN a returning account WHEN completing the sign in THEN pulls its profile and restores it`() = runTest {
        // Given
        prepareScenario(
            sessionUser = googleUser,
            storedProfile = pierre,
        )

        // When
        val outcome = useCase()

        // Then
        assertEquals(listOf(googleUser.userId), profile.downloadedUserIds)
        assertEquals(Result.success(SignInOutcome.PROFILE_RESTORED), outcome)
    }

    @Test
    fun `GIVEN a new account WHEN completing the sign in THEN asks for a profile`() = runTest {
        // Given
        prepareScenario(
            sessionUser = googleUser,
            storedProfile = null,
        )

        // When
        val outcome = useCase()

        // Then
        assertEquals(Result.success(SignInOutcome.NEEDS_PROFILE), outcome)
    }

    @Test
    fun `GIVEN the profile pull fails WHEN completing the sign in THEN fails instead of asking for a profile`() =
        runTest {
            // Given
            val error = IllegalStateException("offline")
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = null,
                pullError = error,
            )

            // When
            val outcome = useCase()

            // Then
            assertEquals(Result.failure(error), outcome)
        }

    @Test
    fun `GIVEN no session WHEN completing the sign in THEN pulls nothing`() = runTest {
        // Given
        prepareScenario(
            sessionUser = null,
            storedProfile = null,
        )

        // When
        useCase()

        // Then
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    private fun prepareScenario(
        sessionUser: SessionUser?,
        storedProfile: UserProfile?,
        pullError: Throwable? = null,
    ) {
        account = FakeAccountRepository().also { it.sessionUser = sessionUser }
        profile = FakeProfileRepository(storedProfile).also { it.pullError = pullError }
        useCase = CompleteSignInUseCase(
            accountRepository = account,
            profileRepository = profile,
        )
    }
}

package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.model.SessionUser
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CompleteSignInUseCase
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    private lateinit var firstDownload: FirstDownload
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

    @Test
    fun `GIVEN a device with no profile WHEN the sign in brings the profile down THEN the first download is pending`() =
        runTest {
            // Given
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = null,
                serverProfile = pierre,
            )

            // When
            val outcome = useCase()

            // Then
            assertEquals(Result.success(SignInOutcome.PROFILE_RESTORED), outcome)
            assertTrue(firstDownload.observeIsPending().value)
        }

    @Test
    fun `GIVEN a device with no profile WHEN the profile reaches it THEN the first download is already pending`() =
        runTest {
            // Given
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = null,
                serverProfile = pierre,
            )
            val pendingWhenProfileArrived = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                profile.profile.filterNotNull().collect {
                    pendingWhenProfileArrived += firstDownload.observeIsPending().value
                }
            }

            // When
            useCase()

            // Then
            assertEquals(listOf(true), pendingWhenProfileArrived)
        }

    @Test
    fun `GIVEN a device with no profile WHEN the account has none on the server THEN no first download is pending`() =
        runTest {
            // Given
            prepareScenario(
                sessionUser = googleUser,
                storedProfile = null,
            )

            // When
            val outcome = useCase()

            // Then
            assertEquals(Result.success(SignInOutcome.NEEDS_PROFILE), outcome)
            assertFalse(firstDownload.observeIsPending().value)
        }

    @Test
    fun `GIVEN a device that has a profile WHEN completing the sign in THEN no first download is pending`() = runTest {
        // Given
        prepareScenario(
            sessionUser = googleUser,
            storedProfile = pierre,
            serverProfile = pierre,
        )

        // When
        val outcome = useCase()

        // Then
        assertEquals(Result.success(SignInOutcome.PROFILE_RESTORED), outcome)
        assertFalse(firstDownload.observeIsPending().value)
    }

    @Test
    fun `GIVEN no profile and the profile pull fails WHEN signing in THEN no first download is pending`() = runTest {
        // Given
        val error = IllegalStateException("offline")
        prepareScenario(
            sessionUser = googleUser,
            storedProfile = null,
            serverProfile = pierre,
            pullError = error,
        )

        // When
        val outcome = useCase()

        // Then
        assertEquals(Result.failure(error), outcome)
        assertFalse(firstDownload.observeIsPending().value)
    }

    @Test
    fun `GIVEN no profile and no session WHEN completing the sign in THEN no first download is pending`() = runTest {
        // Given
        prepareScenario(
            sessionUser = null,
            storedProfile = null,
        )

        // When
        val outcome = useCase()

        // Then
        assertEquals(Result.success(SignInOutcome.NEEDS_PROFILE), outcome)
        assertFalse(firstDownload.observeIsPending().value)
    }

    private fun prepareScenario(
        sessionUser: SessionUser?,
        storedProfile: UserProfile?,
        serverProfile: UserProfile? = null,
        pullError: Throwable? = null,
    ) {
        account = FakeAccountRepository().also { it.sessionUser = sessionUser }
        profile = FakeProfileRepository(storedProfile).also {
            it.serverProfile = serverProfile
            it.pullError = pullError
        }
        firstDownload = FirstDownload()
        useCase = CompleteSignInUseCase(
            accountRepository = account,
            profileRepository = profile,
            firstDownload = firstDownload,
        )
    }
}

package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncCoordinatorTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var coordinator: SyncCoordinator

    @Test
    fun `GIVEN no profile WHEN syncing THEN does not register with the server`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.registrationCount)
    }

    @Test
    fun `GIVEN a local profile WHEN syncing THEN registers pulls and pushes under the user id`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN an anonymous user after the first sync WHEN syncing THEN issues the recovery code`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        coordinator.syncNow()

        // Then
        assertEquals(1, account.recoveryCodeCreations)
        assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt.value)
    }

    @Test
    fun `GIVEN a Google account WHEN syncing THEN never issues a recovery code`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre, status = AccountStatus.Connected("user", AccountProvider.GOOGLE))

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.recoveryCodeCreations)
    }

    @Test
    fun `GIVEN the code was already shown WHEN syncing THEN does not issue another`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre, prompt = RecoveryCodePrompt.SAVED)

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.recoveryCodeCreations)
    }

    @Test
    fun `GIVEN the push fails WHEN syncing THEN returns the failure and issues no code`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, account.recoveryCodeCreations)
    }

    @Test
    fun `GIVEN a snoozed prompt WHEN starting THEN makes it due again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre, prompt = RecoveryCodePrompt.SNOOZED, online = false)

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt.value)
    }

    @Test
    fun `GIVEN an offline start WHEN the device comes online THEN syncs`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre, online = false)
        coordinator.start()
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(1, profile.uploadedUserIds.size)
    }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photoUrl = null)

    private fun TestScope.prepareScenario(
        storedProfile: UserProfile?,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        online: Boolean = true,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.recoveryCodePrompt.value = prompt }
        profile = FakeProfileRepository(initialProfile = storedProfile)
        isOnline = MutableStateFlow(online)
        coordinator = SyncCoordinator(
            accountRepository = account,
            profileRepository = profile,
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            scope = backgroundScope,
        )
    }
}

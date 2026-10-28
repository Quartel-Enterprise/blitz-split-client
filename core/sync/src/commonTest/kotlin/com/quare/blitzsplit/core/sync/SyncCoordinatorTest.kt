package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncCoordinatorTest {
    private val paymentKeys = FakePaymentKeyRepository()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var coordinator: SyncCoordinator
    private lateinit var pullGate: CompletableDeferred<Unit>

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
        assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN a Google account WHEN syncing THEN never issues a recovery code`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = AccountStatus.Connected("user", AccountProvider.GOOGLE),
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.recoveryCodeCreations)
    }

    @Test
    fun `GIVEN the code was already shown WHEN syncing THEN does not issue another`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            prompt = RecoveryCodePrompt.SAVED,
        )

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
        prepareScenario(
            storedProfile = pierre,
            prompt = RecoveryCodePrompt.SNOOZED,
            online = false,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN a saved code left open for review WHEN starting THEN keeps it saved`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            prompt = RecoveryCodePrompt.REVIEW,
            online = false,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(RecoveryCodePrompt.SAVED, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN an offline start WHEN the device comes online THEN syncs`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(1, profile.uploadedUserIds.size)
    }

    @Test
    fun `GIVEN an anonymous user WHEN switching to the Google user of another device THEN syncs under it`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(listOf(OTHER_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(OTHER_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN a Google user WHEN switching to another Apple user THEN syncs under the new user`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.APPLE)
        runCurrent()

        // Then
        assertEquals(listOf(OTHER_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(OTHER_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN an anonymous user WHEN a recovery code restores another one THEN syncs under it`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.Anonymous(FakeAccountRepository.RESTORED_USER_ID)
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN an online start already synced WHEN switching to another user THEN syncs again under it`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID, OTHER_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID, OTHER_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN the other user profile fails to come down WHEN switching THEN pushes nothing over it`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()
        profile.pullError = IllegalStateException("server down")

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(listOf(OTHER_USER_ID), profile.downloadedUserIds)
        assertTrue(profile.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a device not registered yet WHEN the first sync registers it THEN syncs only once`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN an anonymous user WHEN Google links to the same user THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.Connected(FakeAccountRepository.ANONYMOUS_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN an anonymous user offline WHEN starting THEN does not sync for the current user`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN an anonymous user WHEN signing out THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        account.status = AccountStatus.LocalOnly
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a user who signed out WHEN signing in to another user THEN leaves the pull to the sign in screen`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                online = false,
            )
            coordinator.start()
            runCurrent()
            account.status = AccountStatus.LocalOnly
            runCurrent()

            // When
            account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
            runCurrent()

            // Then
            assertEquals(0, account.registrationCount)
            assertTrue(profile.downloadedUserIds.isEmpty())
        }

    @Test
    fun `GIVEN a synced profile with nothing pending WHEN observing the sync state THEN it is synced`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.syncNow()

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.SYNCED, state)
    }

    @Test
    fun `GIVEN a profile saved offline on the first launch WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                online = false,
            )
            coordinator.start()
            runCurrent()
            profile.saveProfile(pierre)
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN a pending change WHEN the sync pushes it THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a pending change and the push fails WHEN syncing THEN the sync state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )
        profile.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the server does not register the device WHEN syncing THEN the sync state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )
        account.registerError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN nothing pending and the pull fails WHEN syncing THEN the sync state is failed`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a sync that failed WHEN a retry succeeds THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )
        profile.pushError = IllegalStateException("server down")
        coordinator.syncNow()
        profile.pushError = null

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN no local profile yet WHEN syncing THEN the sync state is not failed`() = runTest {
        // Given
        prepareScenario(storedProfile = null)

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a pending change WHEN a sync is in flight THEN the sync state is syncing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.SYNCING, state)
    }

    @Test
    fun `GIVEN the last sync failed WHEN another sync is in flight THEN the sync state is syncing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
            pullHangs = true,
        )
        account.registerError = IllegalStateException("server down")
        coordinator.syncNow()
        account.registerError = null
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.SYNCING, state)
    }

    @Test
    fun `GIVEN a sync in flight with a pending change WHEN it is cancelled THEN the sync state is back to pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingChanges = true,
                pullHangs = true,
            )
            val sync = backgroundScope.launch { coordinator.syncNow() }
            runCurrent()

            // When
            sync.cancel()
            runCurrent()

            // Then
            assertEquals(SyncState.PENDING, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN an online start already synced WHEN a change is saved THEN pushes it`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = profile.uploadedUserIds.size

        // When
        profile.saveProfile(pierre.copy(avatarColor = AvatarColor.GREEN))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, profile.uploadedUserIds.size)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a push that failed WHEN retrying after the server is back THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )
        profile.pushError = IllegalStateException("no answer")
        coordinator.syncNow()
        profile.pushError = null

        // When
        coordinator.retry()
        runCurrent()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a push that failed online WHEN another change is saved THEN pushes again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        profile.pushError = IllegalStateException("no answer")
        profile.saveProfile(pierre.copy(avatarColor = AvatarColor.GREEN))
        runCurrent()
        profile.pushError = null
        val uploadsBefore = profile.uploadedUserIds.size

        // When
        profile.saveProfile(pierre.copy(avatarColor = AvatarColor.PEACH))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, profile.uploadedUserIds.size)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a pull that failed with nothing pending WHEN the device goes offline THEN the sync state is synced`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            profile.pullError = IllegalStateException("no answer")
            coordinator.syncNow()

            // When
            isOnline.value = false

            // Then
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )
        profile.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a Google account WHEN signing out THEN goes back to local only and erases the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )

        // When
        coordinator.signOut()

        // Then
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertEquals(1, profile.clearCount)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN the session fails to end WHEN signing out THEN fails and keeps the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )
        account.signOutError = IllegalStateException("database closed")

        // When
        val result = coordinator.signOut()

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, profile.clearCount)
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN an anonymous account WHEN signing out THEN goes back to local only and erases the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
        )

        // When
        coordinator.signOut()

        // Then
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN a sync in flight WHEN signing out THEN erases nothing until it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            pendingChanges = true,
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()

        // When
        backgroundScope.launch { coordinator.signOut() }
        runCurrent()

        // Then
        assertEquals(0, account.signOutCount)
        assertEquals(0, profile.clearCount)
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN a sign out waiting on a sync WHEN the sync ends THEN pushes the change and then erases the copy`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                pendingChanges = true,
                pullHangs = true,
            )
            backgroundScope.launch { coordinator.syncNow() }
            runCurrent()
            backgroundScope.launch { coordinator.signOut() }
            runCurrent()

            // When
            pullGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN a sign out waiting on a sync WHEN the screen that asked leaves THEN still erases the copy`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()
        val caller = backgroundScope.launch { coordinator.signOut() }
        runCurrent()

        // When
        caller.cancel()
        pullGate.complete(Unit)
        runCurrent()

        // Then
        assertEquals(1, account.signOutCount)
        assertEquals(1, profile.clearCount)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN a queued change online WHEN signing out once synced THEN pushes it and signs out`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
        )

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
        assertFalse(account.signOutWhenSynced)
    }

    @Test
    fun `GIVEN an anonymous account with a queued change WHEN signing out once synced THEN pushes it and stays`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                pendingChanges = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.uploadedUserIds)
            assertEquals(0, account.signOutCount)
            assertEquals(anonymous, account.status)
            assertEquals(pierre, profile.profile.value)
        }

    @Test
    fun `GIVEN the push fails WHEN signing out once synced THEN keeps the session the profile and the schedule`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
            )
            profile.pushError = IllegalStateException("server down")

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(0, profile.clearCount)
            assertEquals(pierre, profile.profile.value)
            assertTrue(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN the device is offline WHEN signing out once synced THEN records the schedule and erases nothing`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
                online = false,
            )
            profile.pullError = noConnection

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertTrue(account.signOutWhenSynced)
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(pierre, profile.profile.value)
            assertTrue(profile.hasPendingChanges.value)
        }

    @Test
    fun `GIVEN a sign out scheduled offline WHEN the device comes online THEN pushes the change and signs out`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
                online = false,
            )
            coordinator.start()
            runCurrent()
            profile.pullError = noConnection
            coordinator.signOutWhenSynced()
            runCurrent()
            profile.pullError = null

            // When
            isOnline.value = true
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN a sign out scheduled and a push that failed WHEN a retry succeeds THEN signs out`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
        )
        profile.pushError = IllegalStateException("server down")
        coordinator.signOutWhenSynced()
        runCurrent()
        profile.pushError = null

        // When
        coordinator.retry()
        runCurrent()

        // Then
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN a sign out scheduled on an earlier launch WHEN the app starts online THEN pushes and signs out`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
                signOutScheduled = true,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN a change queued while the push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
                changeQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(2, profile.uploadedUserIds.size)
            assertEquals(1, account.signOutCount)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN a sign out scheduled WHEN cancelling it THEN the schedule is off`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
            signOutScheduled = true,
        )

        // When
        coordinator.cancelSignOutWhenSynced()

        // Then
        assertFalse(account.signOutWhenSynced)
    }

    @Test
    fun `GIVEN a cancelled offline sign out WHEN the device comes online THEN syncs without signing out`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
            online = false,
        )
        coordinator.start()
        runCurrent()
        profile.pullError = noConnection
        coordinator.signOutWhenSynced()
        runCurrent()
        profile.pullError = null
        coordinator.cancelSignOutWhenSynced()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
        assertEquals(0, account.signOutCount)
        assertEquals(google, account.status)
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN no sign out scheduled WHEN a sync succeeds THEN keeps the session and the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, account.signOutCount)
        assertEquals(0, profile.clearCount)
        assertEquals(google, account.status)
        assertEquals(pierre, profile.profile.value)
    }

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val noConnection = IllegalStateException("no connection")

    private fun TestScope.prepareScenario(
        storedProfile: UserProfile?,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        online: Boolean = true,
        pendingChanges: Boolean = false,
        pullHangs: Boolean = false,
        signOutScheduled: Boolean = false,
        changeQueuedDuringFirstPush: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status).also {
            it.recoveryCodePrompt = prompt
            it.signOutWhenSynced = signOutScheduled
        }
        profile =
            FakeProfileRepository(initialProfile = storedProfile).also { it.hasPendingChanges.value = pendingChanges }
        isOnline = MutableStateFlow(online)
        pullGate = CompletableDeferred()
        coordinator = SyncCoordinator(
            accountRepository = account,
            profileRepository = when {
                pullHangs -> GatedPullProfileRepository(profile, pullGate)
                changeQueuedDuringFirstPush -> ChangeDuringFirstPushProfileRepository(profile)
                else -> profile
            },
            paymentKeyRepository = paymentKeys,
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            scope = backgroundScope,
        )
    }

    private companion object {
        const val OTHER_USER_ID = "user-of-another-device"
    }
}

// Why: holds a sync mid-flight until the gate opens, so a test can read the state while it runs,
// cancel it, or let it finish.
private class GatedPullProfileRepository(
    private val delegate: FakeProfileRepository,
    private val gate: CompletableDeferred<Unit>,
) : ProfileRepository by delegate {
    override suspend fun downloadProfile(userId: String): Result<Unit> {
        gate.await()
        return delegate.downloadProfile(userId)
    }
}

// Why: a change saved while the first push is in flight, so the queue is not empty once it ends.
private class ChangeDuringFirstPushProfileRepository(
    private val delegate: FakeProfileRepository,
) : ProfileRepository by delegate {
    override suspend fun uploadPendingProfile(userId: String): Result<Unit> {
        val result = delegate.uploadPendingProfile(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.hasPendingChanges.value = true
        return result
    }
}

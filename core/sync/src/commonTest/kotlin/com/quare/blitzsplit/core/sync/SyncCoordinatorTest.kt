package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
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
    private val calls = mutableListOf<String>()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var notificationPreferences: FakeNotificationPreferencesRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
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

    @Test
    fun `GIVEN a local profile WHEN syncing THEN pulls and pushes the keys after the profile under the same user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingKeys = true,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            val userId = FakeAccountRepository.ANONYMOUS_USER_ID
            assertEquals(
                listOf(
                    "profile download $userId",
                    "profile upload $userId",
                    "keys download $userId",
                    "keys upload $userId",
                ),
                callsWithoutPreferencesAndGroups(),
            )
        }

    @Test
    fun `GIVEN no profile and a pending key WHEN syncing THEN neither pulls nor pushes the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertTrue(paymentKeys.downloadedUserIds.isEmpty())
        assertTrue(paymentKeys.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the keys fail to come down WHEN syncing THEN fails pushes no key and the sync state is failed`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingKeys = true,
            )
            paymentKeys.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertTrue(paymentKeys.uploadedUserIds.isEmpty())
            assertEquals(SyncState.FAILED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN an anonymous user whose keys fail to come down WHEN syncing THEN still issues the recovery code`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            paymentKeys.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.recoveryCodeCreations)
        }

    @Test
    fun `GIVEN a pending key and its push fails WHEN syncing THEN fails and the sync state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingKeys = true,
        )
        paymentKeys.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the profile fails to come down WHEN syncing THEN does not touch the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingKeys = true,
        )
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(paymentKeys.downloadedUserIds.isEmpty())
        assertTrue(paymentKeys.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the profile push fails WHEN syncing THEN does not touch the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
            pendingKeys = true,
        )
        profile.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(paymentKeys.downloadedUserIds.isEmpty())
        assertTrue(paymentKeys.uploadedUserIds.isEmpty())
        assertTrue(paymentKeys.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN an anonymous user WHEN switching to the Google user of another device THEN syncs the keys under it`() =
        runTest {
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
            assertEquals(listOf(OTHER_USER_ID), paymentKeys.downloadedUserIds)
            assertEquals(listOf(OTHER_USER_ID), paymentKeys.uploadedUserIds)
        }

    @Test
    fun `GIVEN the other user keys fail to come down WHEN switching THEN pushes no key over them`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
        )
        coordinator.start()
        runCurrent()
        paymentKeys.pullError = IllegalStateException("server down")

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(listOf(OTHER_USER_ID), paymentKeys.downloadedUserIds)
        assertTrue(paymentKeys.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a synced profile and a key saved offline WHEN observing the sync state THEN it is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        paymentKeys.saveKeys(listOf(pixKey))
        runCurrent()

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.PENDING, state)
    }

    @Test
    fun `GIVEN only a key pending online before any sync WHEN observing the sync state THEN it is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.PENDING, state)
    }

    @Test
    fun `GIVEN a pending key WHEN the sync pushes it THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a key push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )
        paymentKeys.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN a key is saved THEN pushes the keys`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = paymentKeys.uploadedUserIds.size

        // When
        paymentKeys.saveKeys(listOf(pixKey))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, paymentKeys.uploadedUserIds.size)
        assertFalse(paymentKeys.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN a key is deleted THEN pushes the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
        )
        coordinator.start()
        runCurrent()
        val uploadsBefore = paymentKeys.uploadedUserIds.size

        // When
        paymentKeys.deleteKey(
            id = pixKey.id,
            nextDefault = null,
        )
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, paymentKeys.uploadedUserIds.size)
        assertFalse(paymentKeys.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a key push that failed online WHEN another key is saved THEN pushes the keys again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        paymentKeys.pushError = IllegalStateException("no answer")
        paymentKeys.saveKeys(listOf(pixKey))
        runCurrent()
        paymentKeys.pushError = null
        val uploadsBefore = paymentKeys.uploadedUserIds.size

        // When
        paymentKeys.saveKeys(listOf(lightningKey))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, paymentKeys.uploadedUserIds.size)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a key saved offline WHEN the device comes online THEN pushes the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        paymentKeys.saveKeys(listOf(pixKey))
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN no profile online WHEN a key is saved THEN does not sync`() = runTest {
        // Given
        prepareScenario(storedProfile = null)
        coordinator.start()
        runCurrent()

        // When
        paymentKeys.saveKeys(listOf(pixKey))
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(paymentKeys.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a key saved while the keys push is in flight WHEN retrying THEN pushes the keys again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            keyQueuedDuringFirstPush = true,
        )

        // When
        coordinator.retry()
        runCurrent()

        // Then
        assertEquals(2, paymentKeys.uploadedUserIds.size)
        assertFalse(paymentKeys.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a Google account with keys WHEN signing out THEN erases the keys before the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
        )

        // When
        coordinator.signOut()

        // Then
        assertEquals(1, paymentKeys.clearCount)
        assertTrue(paymentKeys.keys.value.isEmpty())
        assertEquals(listOf("account sign out", "keys clear", "profile clear"), callsWithoutPreferencesAndGroups())
    }

    @Test
    fun `GIVEN the session fails to end WHEN signing out THEN keeps the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
        )
        account.signOutError = IllegalStateException("database closed")

        // When
        coordinator.signOut()

        // Then
        assertEquals(0, paymentKeys.clearCount)
        assertEquals(listOf(pixKey), paymentKeys.keys.value)
    }

    @Test
    fun `GIVEN only a key queued online WHEN signing out once synced THEN pushes it and erases the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), paymentKeys.uploadedUserIds)
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertTrue(paymentKeys.keys.value.isEmpty())
        assertFalse(account.signOutWhenSynced)
    }

    @Test
    fun `GIVEN the key push fails WHEN signing out once synced THEN keeps the session the keys and the schedule`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                pendingKeys = true,
            )
            paymentKeys.pushError = IllegalStateException("server down")

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(0, paymentKeys.clearCount)
            assertEquals(listOf(pixKey), paymentKeys.keys.value)
            assertTrue(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a sign out scheduled and a key push that failed WHEN a retry succeeds THEN signs out`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )
        paymentKeys.pushError = IllegalStateException("server down")
        coordinator.signOutWhenSynced()
        runCurrent()
        paymentKeys.pushError = null

        // When
        coordinator.retry()
        runCurrent()

        // Then
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertTrue(paymentKeys.keys.value.isEmpty())
    }

    @Test
    fun `GIVEN a key queued while the keys push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                pendingKeys = true,
                keyQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId, userId), paymentKeys.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            val withoutPreferences = callsWithoutPreferencesAndGroups()
            assertEquals(
                "account sign out",
                withoutPreferences[
                    withoutPreferences.lastIndexOf("keys upload $userId") +
                        1,
                ],
            )
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN a launch WHEN starting THEN keeps the Apple tokens of the session`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(1, account.keepAppleTokensCount)
    }

    @Test
    fun `GIVEN the coordinator already started WHEN starting again THEN keeps the Apple tokens only once`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(1, account.keepAppleTokensCount)
    }

    @Test
    fun `GIVEN a Google account with a profile and keys WHEN deleting the account THEN deletes it on the server`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey, lightningKey),
            )

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, account.deleteAccountCount)
        }

    @Test
    fun `GIVEN a Google account with a profile and keys WHEN deleting the account THEN erases the device copy`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey, lightningKey),
            )
            account.email = "pierre@gmail.com"
            account.recoveryCode = "K7QM4XPA9TRE"

            // When
            coordinator.deleteAccount()

            // Then
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(account.email)
            assertNull(account.recoveryCode)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN an anonymous account WHEN deleting the account THEN goes back to no account and erases the profile`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                storedKeys = listOf(pixKey),
            )

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, account.deleteAccountCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN an account WHEN deleting it THEN the server goes first and then the keys and the profile`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
        )

        // When
        coordinator.deleteAccount()

        // Then
        assertEquals(
            listOf("account delete", "keys clear", "profile clear", "account end session"),
            callsWithoutPreferencesAndGroups(),
        )
    }

    @Test
    fun `GIVEN clearing the keys fails WHEN deleting the account THEN still erases the profile and ends the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
            )
            paymentKeys.clearError = IllegalStateException("disk full")

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertNull(profile.profile.value)
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN the server fails to delete WHEN deleting the account THEN fails and keeps every local data`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
        )
        account.email = "pierre@gmail.com"
        account.deleteAccountError = noConnection

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, account.signOutCount)
        assertEquals(google, account.status)
        assertEquals("pierre@gmail.com", account.email)
        assertEquals(0, profile.clearCount)
        assertEquals(pierre, profile.profile.value)
        assertEquals(0, paymentKeys.clearCount)
        assertEquals(listOf(pixKey), paymentKeys.keys.value)
    }

    @Test
    fun `GIVEN the device is offline WHEN deleting the account THEN fails and keeps the profile and the keys`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                online = false,
                storedKeys = listOf(pixKey),
            )
            account.deleteAccountError = noConnection

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isFailure)
            assertEquals(anonymous, account.status)
            assertEquals(pierre, profile.profile.value)
            assertEquals(listOf(pixKey), paymentKeys.keys.value)
        }

    @Test
    fun `GIVEN a sync in flight WHEN deleting the account THEN neither deletes nor erases until it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()

        // When
        backgroundScope.launch { coordinator.deleteAccount() }
        runCurrent()

        // Then
        assertEquals(0, account.deleteAccountCount)
        assertEquals(0, account.signOutCount)
        assertEquals(pierre, profile.profile.value)
        assertEquals(listOf(pixKey), paymentKeys.keys.value)
    }

    @Test
    fun `GIVEN a deletion waiting on a sync WHEN the sync ends THEN deletes the account and erases the copy`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                pullHangs = true,
            )
            backgroundScope.launch { coordinator.syncNow() }
            runCurrent()
            backgroundScope.launch { coordinator.deleteAccount() }
            runCurrent()

            // When
            pullGate.complete(Unit)
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            val withoutPreferences = callsWithoutPreferencesAndGroups()
            assertEquals("account delete", withoutPreferences[withoutPreferences.indexOf("keys upload $userId") + 1])
            assertEquals(1, account.deleteAccountCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN a deletion waiting on a sync WHEN the screen that asked leaves THEN still deletes and erases`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                pullHangs = true,
            )
            backgroundScope.launch { coordinator.syncNow() }
            runCurrent()
            val caller = backgroundScope.launch { coordinator.deleteAccount() }
            runCurrent()

            // When
            caller.cancel()
            pullGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(1, account.deleteAccountCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN a deleted account WHEN erasing the device copy THEN skips the sign out the server would refuse`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
            )

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN a sync in flight WHEN unlinking THEN waits for it before removing the identity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()
        backgroundScope.launch { coordinator.removeSocialIdentity() }
        runCurrent()
        val unlinksDuringSync = account.unlinkCount

        // When
        pullGate.complete(Unit)
        runCurrent()

        // Then
        assertEquals(0, unlinksDuringSync)
        assertEquals(1, account.unlinkCount)
        assertEquals(AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID), account.status)
    }

    @Test
    fun `GIVEN an unlink waiting on a sync WHEN the screen that asked leaves THEN still unlinks`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pullHangs = true,
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()
        val caller = backgroundScope.launch { coordinator.removeSocialIdentity() }
        runCurrent()

        // When
        caller.cancel()
        pullGate.complete(Unit)
        runCurrent()

        // Then
        assertEquals(1, account.unlinkCount)
        assertEquals(AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID), account.status)
    }

    @Test
    fun `GIVEN a local profile WHEN syncing THEN syncs the preferences after the profile under the same user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            val userId = FakeAccountRepository.ANONYMOUS_USER_ID
            assertEquals(
                listOf(
                    "profile download $userId",
                    "profile upload $userId",
                    "preferences download $userId",
                    "preferences upload $userId",
                ),
                callsWithoutKeysAndGroups(),
            )
            assertFalse(notificationPreferences.hasPendingChanges.value)
        }

    @Test
    fun `GIVEN no profile and pending preferences WHEN syncing THEN neither pulls nor pushes the preferences`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                pendingPreferences = true,
            )

            // When
            coordinator.syncNow()

            // Then
            assertTrue(notificationPreferences.downloadedUserIds.isEmpty())
            assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
        }

    @Test
    fun `GIVEN the preferences fail to come down WHEN syncing THEN fails pushes none and the state is failed`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )
            notificationPreferences.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
            assertTrue(notificationPreferences.hasPendingChanges.value)
            assertEquals(SyncState.FAILED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN the preferences fail to come down WHEN syncing THEN still pulls and pushes the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
        )
        notificationPreferences.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN an anonymous user whose preferences fail to come down WHEN syncing THEN issues the code`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        notificationPreferences.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, account.recoveryCodeCreations)
        assertEquals(RecoveryCodePrompt.DUE, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN pending preferences and their push fails WHEN syncing THEN fails and the sync state is failed`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )
            notificationPreferences.pushError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(SyncState.FAILED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN the preferences push fails WHEN syncing THEN still pushes the keys`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
        )
        notificationPreferences.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN an anonymous user whose preferences push fails WHEN syncing THEN still issues the recovery code`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )
            notificationPreferences.pushError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.recoveryCodeCreations)
        }

    @Test
    fun `GIVEN the keys fail to come down WHEN syncing THEN still pulls and pushes the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPreferences = true,
        )
        paymentKeys.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the key push fails WHEN syncing THEN still pushes the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
        )
        paymentKeys.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the profile fails to come down WHEN syncing THEN does not touch the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPreferences = true,
        )
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(notificationPreferences.downloadedUserIds.isEmpty())
        assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the profile push fails WHEN syncing THEN does not touch the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
            pendingPreferences = true,
        )
        profile.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(notificationPreferences.downloadedUserIds.isEmpty())
        assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
        assertTrue(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a sign out scheduled and the preferences fail to come down WHEN syncing THEN still signs out`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                signOutScheduled = true,
            )
            notificationPreferences.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN an anonymous user WHEN switching to another Google user THEN syncs the preferences under it`() =
        runTest {
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
            assertEquals(listOf(OTHER_USER_ID), notificationPreferences.downloadedUserIds)
            assertEquals(listOf(OTHER_USER_ID), notificationPreferences.uploadedUserIds)
        }

    @Test
    fun `GIVEN the other user preferences fail to come down WHEN switching THEN pushes no preference over them`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                online = false,
                pendingPreferences = true,
            )
            coordinator.start()
            runCurrent()
            notificationPreferences.pullError = IllegalStateException("server down")

            // When
            account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
            runCurrent()

            // Then
            assertEquals(listOf(OTHER_USER_ID), notificationPreferences.downloadedUserIds)
            assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
        }

    @Test
    fun `GIVEN a synced profile and a preference changed offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            notificationPreferences.updatePreference(
                type = NotificationType.INVITES,
                isEnabled = true,
            )
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN only the preferences pending online before any sync WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN pending preferences WHEN the sync pushes them THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPreferences = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a preferences push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPreferences = true,
        )
        notificationPreferences.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN a preference is changed THEN pushes the preferences`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = notificationPreferences.uploadedUserIds.size
        val profileUploadsBefore = profile.uploadedUserIds.size

        // When
        notificationPreferences.updatePreference(
            type = NotificationType.NEW_EXPENSE,
            isEnabled = false,
        )
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, notificationPreferences.uploadedUserIds.size)
        assertEquals(profileUploadsBefore + 1, profile.uploadedUserIds.size)
        assertFalse(notificationPreferences.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a preferences push that failed online WHEN another preference is changed THEN pushes them again`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            notificationPreferences.pushError = IllegalStateException("no answer")
            notificationPreferences.updatePreference(
                type = NotificationType.INVITES,
                isEnabled = true,
            )
            runCurrent()
            notificationPreferences.pushError = null
            val uploadsBefore = notificationPreferences.uploadedUserIds.size

            // When
            notificationPreferences.updatePreference(
                type = NotificationType.SETTLE_REMINDERS,
                isEnabled = false,
            )
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, notificationPreferences.uploadedUserIds.size)
            assertFalse(notificationPreferences.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a preference changed offline WHEN the device comes online THEN pushes the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        notificationPreferences.updatePreference(
            type = NotificationType.INVITES,
            isEnabled = true,
        )
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a profile offline WHEN a preference is changed THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        notificationPreferences.updatePreference(
            type = NotificationType.INVITES,
            isEnabled = true,
        )
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
        assertTrue(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN no profile online WHEN a preference is changed THEN does not sync`() = runTest {
        // Given
        prepareScenario(storedProfile = null)
        coordinator.start()
        runCurrent()

        // When
        notificationPreferences.updatePreference(
            type = NotificationType.INVITES,
            isEnabled = true,
        )
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(notificationPreferences.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a preference changed while the preferences push is in flight WHEN retrying THEN pushes them again`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
                preferenceQueuedDuringFirstPush = true,
            )

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertEquals(2, notificationPreferences.uploadedUserIds.size)
            assertFalse(notificationPreferences.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a Google account with changed preferences WHEN signing out THEN erases the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPreferences = invitesOnly,
        )

        // When
        val result = coordinator.signOut()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, notificationPreferences.clearCount)
        assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
        assertEquals("account sign out", calls.first())
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN the session fails to end WHEN signing out THEN keeps the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPreferences = invitesOnly,
        )
        account.signOutError = IllegalStateException("database closed")

        // When
        coordinator.signOut()

        // Then
        assertEquals(0, notificationPreferences.clearCount)
        assertEquals(invitesOnly, notificationPreferences.preferences.value)
    }

    @Test
    fun `GIVEN only the preferences queued online WHEN signing out once synced THEN pushes them and erases them`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPreferences = invitesOnly,
                pendingPreferences = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), notificationPreferences.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
            assertFalse(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN the preferences push fails WHEN signing out once synced THEN keeps the session and the preferences`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPreferences = invitesOnly,
                pendingPreferences = true,
            )
            notificationPreferences.pushError = IllegalStateException("server down")

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(0, notificationPreferences.clearCount)
            assertEquals(invitesOnly, notificationPreferences.preferences.value)
            assertEquals(pierre, profile.profile.value)
            assertTrue(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a sign out scheduled and a preferences push that failed WHEN a retry succeeds THEN signs out`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPreferences = invitesOnly,
                pendingPreferences = true,
            )
            notificationPreferences.pushError = IllegalStateException("server down")
            coordinator.signOutWhenSynced()
            runCurrent()
            notificationPreferences.pushError = null

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
        }

    @Test
    fun `GIVEN a preference changed while the push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingPreferences = true,
                preferenceQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId, userId), notificationPreferences.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("preferences upload $userId"))
        }

    @Test
    fun `GIVEN a Google account with changed preferences WHEN deleting the account THEN erases the preferences`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPreferences = invitesOnly,
            )

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, notificationPreferences.clearCount)
            assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
        }

    @Test
    fun `GIVEN an account WHEN deleting it THEN the keys are erased before the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            storedPreferences = invitesOnly,
        )

        // When
        coordinator.deleteAccount()

        // Then
        assertEquals(listOf("account delete", "keys clear"), calls.take(2))
        assertTrue("preferences clear" in calls)
        assertEquals("account end session", calls.last())
    }

    @Test
    fun `GIVEN clearing the preferences fails WHEN deleting the account THEN still erases the keys and the profile`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                storedPreferences = invitesOnly,
            )
            notificationPreferences.clearError = IllegalStateException("disk full")

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertNull(profile.profile.value)
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN clearing the keys fails WHEN deleting the account THEN still erases the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            storedPreferences = invitesOnly,
        )
        paymentKeys.clearError = IllegalStateException("disk full")

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
    }

    @Test
    fun `GIVEN the server fails to delete WHEN deleting the account THEN keeps the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPreferences = invitesOnly,
        )
        account.deleteAccountError = noConnection

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, notificationPreferences.clearCount)
        assertEquals(invitesOnly, notificationPreferences.preferences.value)
    }

    @Test
    fun `GIVEN a local profile WHEN syncing THEN syncs the groups after the profile under the same user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingGroups = true,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            val userId = FakeAccountRepository.ANONYMOUS_USER_ID
            assertEquals(
                listOf(
                    "profile download $userId",
                    "profile upload $userId",
                    "groups download $userId",
                    "groups upload $userId",
                ),
                callsWithoutKeysAndPreferences(),
            )
            assertFalse(groups.hasPendingChanges.value)
        }

    @Test
    fun `GIVEN a Google account WHEN syncing THEN pulls and pushes the groups under its user id`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingGroups = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), groups.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), groups.uploadedUserIds)
    }

    @Test
    fun `GIVEN no profile and pending groups WHEN syncing THEN neither pulls nor pushes the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            storedGroups = listOf(trip),
            pendingGroups = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN fails pushes none and the state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
        )
        groups.pullError = IllegalStateException("relation groups does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.uploadedUserIds.isEmpty())
        assertTrue(groups.hasPendingChanges.value)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN pending groups and their push fails WHEN syncing THEN fails keeps them pending and is failed`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingGroups = true,
            )
            groups.pushError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertTrue(groups.hasPendingChanges.value)
            assertEquals(SyncState.FAILED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN still syncs the keys and the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
        )
        groups.pullError = IllegalStateException("relation groups does not exist")

        // When
        coordinator.syncNow()

        // Then
        val userId = FakeAccountRepository.ANONYMOUS_USER_ID
        assertEquals(listOf(userId), paymentKeys.downloadedUserIds)
        assertEquals(listOf(userId), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
        assertEquals(listOf(userId), notificationPreferences.downloadedUserIds)
        assertEquals(listOf(userId), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the groups push fails WHEN syncing THEN still pushes the keys and the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
            pendingGroups = true,
        )
        groups.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the keys fail to come down WHEN syncing THEN still pulls and pushes the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
        )
        paymentKeys.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.uploadedUserIds)
        assertFalse(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the preferences push fails WHEN syncing THEN still pulls and pushes the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPreferences = true,
            pendingGroups = true,
        )
        notificationPreferences.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.uploadedUserIds)
        assertFalse(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the profile fails to come down WHEN syncing THEN does not touch the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
        )
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the profile push fails WHEN syncing THEN does not touch the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
            pendingGroups = true,
        )
        profile.pushError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
        assertTrue(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN an anonymous user whose groups fail to come down WHEN syncing THEN still issues the recovery code`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            groups.pullError = IllegalStateException("relation groups does not exist")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.recoveryCodeCreations)
        }

    @Test
    fun `GIVEN an anonymous user WHEN switching to another Google user THEN syncs the groups under it`() = runTest {
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
        assertEquals(listOf(OTHER_USER_ID), groups.downloadedUserIds)
        assertEquals(listOf(OTHER_USER_ID), groups.uploadedUserIds)
    }

    @Test
    fun `GIVEN the other user groups fail to come down WHEN switching THEN pushes no group over them`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            online = false,
            pendingGroups = true,
        )
        coordinator.start()
        runCurrent()
        groups.pullError = IllegalStateException("server down")

        // When
        account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
        runCurrent()

        // Then
        assertEquals(listOf(OTHER_USER_ID), groups.downloadedUserIds)
        assertTrue(groups.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a synced profile and a group created offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            groups.createGroup(trip, createdAt = 1L)
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN only pending groups online before any sync WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingGroups = true,
            )

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN pending groups WHEN the sync pushes them THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a groups push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
        )
        groups.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN a group is created THEN pushes the groups`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = groups.uploadedUserIds.size

        // When
        groups.createGroup(trip, createdAt = 1L)
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, groups.uploadedUserIds.size)
        assertFalse(groups.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN only the groups become pending THEN pushes the groups`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = groups.uploadedUserIds.size

        // When
        groups.hasPendingChanges.value = true
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, groups.uploadedUserIds.size)
        assertFalse(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a groups push that failed online WHEN another group is created THEN pushes the groups again`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            groups.pushError = IllegalStateException("no answer")
            groups.createGroup(trip, createdAt = 1L)
            runCurrent()
            groups.pushError = null
            val uploadsBefore = groups.uploadedUserIds.size

            // When
            groups.createGroup(dinner, createdAt = 2L)
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, groups.uploadedUserIds.size)
            assertFalse(groups.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a group created offline WHEN the device comes online THEN pushes the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        groups.createGroup(trip, createdAt = 1L)
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.uploadedUserIds)
        assertFalse(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a profile offline WHEN a group is created THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        groups.createGroup(trip, createdAt = 1L)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
        assertTrue(groups.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN no profile online WHEN a group is created THEN does not sync`() = runTest {
        // Given
        prepareScenario(storedProfile = null)
        coordinator.start()
        runCurrent()

        // When
        groups.createGroup(trip, createdAt = 1L)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a Google account with groups WHEN signing out THEN erases the groups after ending the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedGroups = listOf(trip),
            )

            // When
            val result = coordinator.signOut()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, groups.clearCount)
            assertTrue(groups.groups.value.isEmpty())
            assertEquals("account sign out", calls.first())
            assertTrue("groups clear" in calls)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN the session fails to end WHEN signing out THEN keeps the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
        )
        account.signOutError = IllegalStateException("database closed")

        // When
        coordinator.signOut()

        // Then
        assertEquals(0, groups.clearCount)
        assertEquals(listOf(trip), groups.groups.value)
    }

    @Test
    fun `GIVEN clearing the groups fails WHEN signing out THEN fails after ending the session`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
        )
        groups.clearError = IllegalStateException("disk full")

        // When
        val result = coordinator.signOut()

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
    }

    @Test
    fun `GIVEN only the groups queued online WHEN signing out once synced THEN pushes them and erases them`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedGroups = listOf(trip),
                pendingGroups = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), groups.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(groups.groups.value.isEmpty())
            assertFalse(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN the groups push fails WHEN signing out once synced THEN keeps the session and the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
            pendingGroups = true,
        )
        groups.pushError = IllegalStateException("server down")

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(0, account.signOutCount)
        assertEquals(google, account.status)
        assertEquals(0, groups.clearCount)
        assertEquals(listOf(trip), groups.groups.value)
        assertTrue(groups.hasPendingChanges.value)
        assertEquals(pierre, profile.profile.value)
        assertTrue(account.signOutWhenSynced)
    }

    @Test
    fun `GIVEN a sign out scheduled and a groups push that failed WHEN a retry succeeds THEN signs out`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
            pendingGroups = true,
        )
        groups.pushError = IllegalStateException("server down")
        coordinator.signOutWhenSynced()
        runCurrent()
        groups.pushError = null

        // When
        coordinator.retry()
        runCurrent()

        // Then
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertTrue(groups.groups.value.isEmpty())
    }

    @Test
    fun `GIVEN a group created while the push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedGroups = listOf(trip),
                pendingGroups = true,
                groupQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId, userId), groups.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("groups upload $userId"))
        }

    @Test
    fun `GIVEN a Google account with groups WHEN deleting the account THEN erases the groups after the server`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedGroups = listOf(trip),
            )

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, groups.clearCount)
            assertTrue(groups.groups.value.isEmpty())
            assertEquals("account delete", calls.first())
            assertTrue("groups clear" in calls)
            assertEquals("account end session", calls.last())
        }

    @Test
    fun `GIVEN clearing the groups fails WHEN deleting the account THEN still erases the rest and ends the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                storedPreferences = invitesOnly,
                storedGroups = listOf(trip),
            )
            groups.clearError = IllegalStateException("disk full")

            // When
            val result = coordinator.deleteAccount()

            // Then
            assertTrue(result.isSuccess)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertEquals(NotificationPreferences.DEFAULT, notificationPreferences.preferences.value)
            assertNull(profile.profile.value)
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN clearing the keys fails WHEN deleting the account THEN still erases the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedKeys = listOf(pixKey),
            storedGroups = listOf(trip),
        )
        paymentKeys.clearError = IllegalStateException("disk full")

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, groups.clearCount)
        assertTrue(groups.groups.value.isEmpty())
    }

    @Test
    fun `GIVEN the server fails to delete WHEN deleting the account THEN keeps the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
        )
        account.deleteAccountError = noConnection

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, groups.clearCount)
        assertEquals(listOf(trip), groups.groups.value)
    }

    @Test
    fun `GIVEN a pending bill WHEN syncing THEN pulls and pushes the bills right after the groups`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
            pendingBills = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        val userId = FakeAccountRepository.ANONYMOUS_USER_ID
        assertEquals(
            listOf(
                "profile download $userId",
                "profile upload $userId",
                "groups download $userId",
                "groups upload $userId",
                "bills download $userId",
                "bills upload $userId",
            ),
            calls.filterNot { it.startsWith("keys ") || it.startsWith("preferences ") },
        )
        assertFalse(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a Google account WHEN syncing THEN pulls and pushes the bills under its user id`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingBills = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), expenses.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), expenses.uploadedUserIds)
    }

    @Test
    fun `GIVEN no profile and a pending bill WHEN syncing THEN neither pulls nor pushes the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            pendingBills = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the profile fails to come down WHEN syncing THEN does not touch the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
        assertTrue(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN neither pulls nor pushes the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )
        groups.pullError = IllegalStateException("relation groups does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
        assertTrue(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the groups push fails WHEN syncing THEN neither pulls nor pushes the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
            pendingBills = true,
        )
        groups.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
        assertTrue(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the bills fail to come down WHEN syncing THEN fails pushes none and the state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )
        expenses.pullError = IllegalStateException("relation expenses does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), expenses.downloadedUserIds)
        assertTrue(expenses.uploadedUserIds.isEmpty())
        assertTrue(expenses.hasPendingChanges.value)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a pending bill and its push fails WHEN syncing THEN fails keeps it pending and is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )
        expenses.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), expenses.uploadedUserIds)
        assertTrue(expenses.hasPendingChanges.value)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the bills fail to come down WHEN syncing THEN still syncs the keys and the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
        )
        expenses.pullError = IllegalStateException("relation expenses does not exist")

        // When
        coordinator.syncNow()

        // Then
        val userId = FakeAccountRepository.ANONYMOUS_USER_ID
        assertEquals(listOf(userId), paymentKeys.uploadedUserIds)
        assertFalse(paymentKeys.hasPendingChanges.value)
        assertEquals(listOf(userId), notificationPreferences.uploadedUserIds)
        assertFalse(notificationPreferences.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN an anonymous user whose bills fail to come down WHEN syncing THEN still issues the recovery code`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            expenses.pullError = IllegalStateException("relation expenses does not exist")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.recoveryCodeCreations)
        }

    @Test
    fun `GIVEN a sync that failed on the bills WHEN a retry succeeds THEN pushes them and the state is synced`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingBills = true,
            )
            expenses.pushError = IllegalStateException("server down")
            coordinator.syncNow()
            expenses.pushError = null

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertFalse(expenses.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN an anonymous user WHEN switching to another Google user THEN syncs the bills under it`() = runTest {
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
        assertEquals(listOf(OTHER_USER_ID), expenses.downloadedUserIds)
        assertEquals(listOf(OTHER_USER_ID), expenses.uploadedUserIds)
    }

    @Test
    fun `GIVEN nothing else pending and a bill saved offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            expenses.hasPendingChanges.value = true
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN only a pending bill online before any sync WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingBills = true,
            )

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN a pending bill WHEN the sync pushes it THEN the sync state is synced`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a bills push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
        )
        expenses.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN an online start already synced WHEN only a bill becomes pending THEN pushes the bills`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = expenses.uploadedUserIds.size

        // When
        expenses.hasPendingChanges.value = true
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, expenses.uploadedUserIds.size)
        assertFalse(expenses.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a bills push that failed online WHEN another bill is saved THEN pushes the bills again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        expenses.pushError = IllegalStateException("no answer")
        expenses.hasPendingChanges.value = true
        expenses.pendingVersions.value = listOf(1_000L)
        runCurrent()
        expenses.pushError = null
        val uploadsBefore = expenses.uploadedUserIds.size

        // When
        expenses.pendingVersions.value = listOf(1_000L, 2_000L)
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, expenses.uploadedUserIds.size)
        assertFalse(expenses.hasPendingChanges.value)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a bill saved offline WHEN the device comes online THEN pushes the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        expenses.hasPendingChanges.value = true
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), expenses.uploadedUserIds)
        assertFalse(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a profile offline WHEN a bill is saved THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        expenses.hasPendingChanges.value = true
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
        assertTrue(expenses.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN no profile online WHEN a bill is saved THEN does not sync`() = runTest {
        // Given
        prepareScenario(storedProfile = null)
        coordinator.start()
        runCurrent()

        // When
        expenses.hasPendingChanges.value = true
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(expenses.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN only a bill queued online WHEN signing out once synced THEN pushes it before ending the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingBills = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId), expenses.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("bills upload $userId"))
        }

    @Test
    fun `GIVEN the bills push fails WHEN signing out once synced THEN keeps the session and the schedule`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedGroups = listOf(trip),
            pendingBills = true,
        )
        expenses.pushError = IllegalStateException("server down")

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(0, account.signOutCount)
        assertEquals(google, account.status)
        assertEquals(0, groups.clearCount)
        assertEquals(pierre, profile.profile.value)
        assertTrue(expenses.hasPendingChanges.value)
        assertTrue(account.signOutWhenSynced)
    }

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val noConnection = IllegalStateException("no connection")
    private val pixKey = PaymentKey(
        id = "pix-email",
        type = PaymentKeyType.EMAIL,
        value = "pierre@example.com",
        isDefault = true,
        addedAt = 1L,
    )
    private val lightningKey = PaymentKey(
        id = "lightning",
        type = PaymentKeyType.LIGHTNING,
        value = "pierre@walletofsatoshi.com",
        isDefault = true,
        addedAt = 2L,
    )

    private val invitesOnly = NotificationPreferences(setOf(NotificationType.INVITES))

    private val trip = group(
        id = "trip",
        name = "Viagem",
    )
    private val dinner = group(
        id = "dinner",
        name = "Jantar",
    )

    private fun group(
        id: String,
        name: String,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "me",
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
    )

    private fun callsWithoutPreferencesAndGroups(): List<String> = calls.filterNot {
        it.startsWith("preferences ") || it.startsWith("groups ") || it.startsWith("bills ")
    }

    private fun callsWithoutKeysAndGroups(): List<String> = calls.filterNot {
        it.startsWith("keys ") || it.startsWith("groups ") || it.startsWith("bills ")
    }

    private fun callsWithoutKeysAndPreferences(): List<String> = calls.filterNot {
        it.startsWith("keys ") || it.startsWith("preferences ") || it.startsWith("bills ")
    }

    private fun TestScope.prepareScenario(
        storedProfile: UserProfile?,
        status: AccountStatus = AccountStatus.LocalOnly,
        prompt: RecoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
        online: Boolean = true,
        pendingChanges: Boolean = false,
        pullHangs: Boolean = false,
        signOutScheduled: Boolean = false,
        changeQueuedDuringFirstPush: Boolean = false,
        storedKeys: List<PaymentKey> = emptyList(),
        pendingKeys: Boolean = false,
        keyQueuedDuringFirstPush: Boolean = false,
        storedPreferences: NotificationPreferences = NotificationPreferences.DEFAULT,
        pendingPreferences: Boolean = false,
        preferenceQueuedDuringFirstPush: Boolean = false,
        storedGroups: List<Group> = emptyList(),
        pendingGroups: Boolean = false,
        groupQueuedDuringFirstPush: Boolean = false,
        pendingBills: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status).also {
            it.recoveryCodePrompt = prompt
            it.signOutWhenSynced = signOutScheduled
        }
        profile =
            FakeProfileRepository(initialProfile = storedProfile).also { it.hasPendingChanges.value = pendingChanges }
        paymentKeys =
            FakePaymentKeyRepository(initialKeys = storedKeys).also { it.hasPendingChanges.value = pendingKeys }
        notificationPreferences = FakeNotificationPreferencesRepository(initialPreferences = storedPreferences).also {
            it.hasPendingChanges.value = pendingPreferences
        }
        groups = FakeGroupRepository(initialGroups = storedGroups).also { it.hasPendingChanges.value = pendingGroups }
        expenses = FakeExpenseRepository().also { it.hasPendingChanges.value = pendingBills }
        isOnline = MutableStateFlow(online)
        pullGate = CompletableDeferred()
        coordinator = SyncCoordinator(
            accountRepository = RecordingAccountRepository(account, calls),
            profileRepository = RecordingProfileRepository(
                delegate = when {
                    pullHangs -> GatedPullProfileRepository(profile, pullGate)
                    changeQueuedDuringFirstPush -> ChangeDuringFirstPushProfileRepository(profile)
                    else -> profile
                },
                calls = calls,
            ),
            paymentKeyRepository = RecordingPaymentKeyRepository(
                delegate = if (keyQueuedDuringFirstPush) {
                    KeyDuringFirstPushPaymentKeyRepository(
                        paymentKeys,
                    )
                } else {
                    paymentKeys
                },
                calls = calls,
            ),
            notificationPreferencesRepository = RecordingNotificationPreferencesRepository(
                delegate = if (preferenceQueuedDuringFirstPush) {
                    PreferenceDuringFirstPushNotificationPreferencesRepository(notificationPreferences)
                } else {
                    notificationPreferences
                },
                calls = calls,
            ),
            groupRepository = RecordingGroupRepository(
                delegate = if (groupQueuedDuringFirstPush) GroupDuringFirstPushGroupRepository(groups) else groups,
                calls = calls,
            ),
            expenseRepository = RecordingExpenseRepository(expenses, calls),
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

// Why: a key saved while the first key push is in flight, so the keys are still pending once it ends.
private class KeyDuringFirstPushPaymentKeyRepository(
    private val delegate: FakePaymentKeyRepository,
) : PaymentKeyRepository by delegate {
    override suspend fun uploadPendingKeys(userId: String): Result<Unit> {
        val result = delegate.uploadPendingKeys(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.hasPendingChanges.value = true
        return result
    }
}

// Why: one log across the account, the profile, the keys, the preferences, the groups and the bills, so a test can
// assert the order of their calls.
private class RecordingAccountRepository(
    private val delegate: AccountRepository,
    private val calls: MutableList<String>,
) : AccountRepository by delegate {
    override suspend fun signOut() {
        calls += "account sign out"
        delegate.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        calls += "account delete"
        return delegate.deleteAccount()
    }

    override suspend fun endDeletedSession() {
        calls += "account end session"
        delegate.endDeletedSession()
    }
}

private class RecordingProfileRepository(
    private val delegate: ProfileRepository,
    private val calls: MutableList<String>,
) : ProfileRepository by delegate {
    override suspend fun downloadProfile(userId: String): Result<Unit> {
        calls += "profile download $userId"
        return delegate.downloadProfile(userId)
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> {
        calls += "profile upload $userId"
        return delegate.uploadPendingProfile(userId)
    }

    override suspend fun clear() {
        calls += "profile clear"
        delegate.clear()
    }
}

private class RecordingPaymentKeyRepository(
    private val delegate: PaymentKeyRepository,
    private val calls: MutableList<String>,
) : PaymentKeyRepository by delegate {
    override suspend fun downloadKeys(userId: String): Result<Unit> {
        calls += "keys download $userId"
        return delegate.downloadKeys(userId)
    }

    override suspend fun uploadPendingKeys(userId: String): Result<Unit> {
        calls += "keys upload $userId"
        return delegate.uploadPendingKeys(userId)
    }

    override suspend fun clear() {
        calls += "keys clear"
        delegate.clear()
    }
}

private class RecordingNotificationPreferencesRepository(
    private val delegate: NotificationPreferencesRepository,
    private val calls: MutableList<String>,
) : NotificationPreferencesRepository by delegate {
    override suspend fun downloadPreferences(userId: String): Result<Unit> {
        calls += "preferences download $userId"
        return delegate.downloadPreferences(userId)
    }

    override suspend fun uploadPendingPreferences(userId: String): Result<Unit> {
        calls += "preferences upload $userId"
        return delegate.uploadPendingPreferences(userId)
    }

    override suspend fun clear() {
        calls += "preferences clear"
        delegate.clear()
    }
}

// Why: a preference changed while the first preferences push is in flight, so they are still pending once it ends.
private class PreferenceDuringFirstPushNotificationPreferencesRepository(
    private val delegate: FakeNotificationPreferencesRepository,
) : NotificationPreferencesRepository by delegate {
    override suspend fun uploadPendingPreferences(userId: String): Result<Unit> {
        val result = delegate.uploadPendingPreferences(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.hasPendingChanges.value = true
        return result
    }
}

private class RecordingGroupRepository(
    private val delegate: GroupRepository,
    private val calls: MutableList<String>,
) : GroupRepository by delegate {
    override suspend fun downloadGroups(userId: String): Result<Unit> {
        calls += "groups download $userId"
        return delegate.downloadGroups(userId)
    }

    override suspend fun uploadPendingGroups(userId: String): Result<Unit> {
        calls += "groups upload $userId"
        return delegate.uploadPendingGroups(userId)
    }

    override suspend fun clear() {
        calls += "groups clear"
        delegate.clear()
    }
}

// Why: a group created while the first groups push is in flight, so the groups are still pending once it ends.
private class GroupDuringFirstPushGroupRepository(
    private val delegate: FakeGroupRepository,
) : GroupRepository by delegate {
    override suspend fun uploadPendingGroups(userId: String): Result<Unit> {
        val result = delegate.uploadPendingGroups(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.hasPendingChanges.value = true
        return result
    }
}

/*
 * Why: FakeExpenseRepository keeps a bill pending after a push that succeeds, while the real repository marks it
 * synced and the coordinator syncs again until nothing is pending.
 */
private class RecordingExpenseRepository(
    private val delegate: FakeExpenseRepository,
    private val calls: MutableList<String>,
) : ExpenseRepository by delegate {
    override suspend fun downloadExpenses(userId: String): Result<Unit> {
        calls += "bills download $userId"
        return delegate.downloadExpenses(userId)
    }

    override suspend fun uploadPendingExpenses(userId: String): Result<Unit> {
        calls += "bills upload $userId"
        return delegate.uploadPendingExpenses(userId)
    }
}

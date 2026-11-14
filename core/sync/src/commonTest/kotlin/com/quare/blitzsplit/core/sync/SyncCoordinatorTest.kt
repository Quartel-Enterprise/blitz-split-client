package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.NotGroupCreatorException
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.provider.crashlytics.ReportableFailure
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class SyncCoordinatorTest {
    private val calls = mutableListOf<String>()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var notificationPreferences: FakeNotificationPreferencesRepository
    private lateinit var appInvite: FakeAppInviteRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var categories: FakeCategoryRepository
    private lateinit var billComments: FakeBillCommentRepository
    private lateinit var billAttachments: FakeBillAttachmentRepository
    private lateinit var pushDevices: FakePushDeviceRepository
    private val categoryTerms = FakeCategoryTermRepository()
    private val customColors = FakeCustomColorRepository()
    private lateinit var activity: FakeActivityRepository
    private lateinit var paymentVersionsAgain: MutableSharedFlow<List<Long>>
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var firstDownload: FirstDownload
    private val remoteChanges = FakeRemoteChangeRepository()
    private val appForeground = AppForeground()
    private lateinit var coordinator: SyncCoordinator
    private lateinit var pullGate: CompletableDeferred<Unit>
    private val firstDownloadProgress = mutableListOf<FirstDownloadProgress?>()
    private val pendingAtGroupsFullDownload = mutableListOf<Boolean>()
    private val reportedFailures = mutableListOf<Throwable>()

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
    fun `GIVEN a term changed on the device WHEN syncing THEN the map comes down and goes up under the user id`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            categoryTerms.saveTerm(
                term = "pizzaria",
                categoryId = "food.restaurant",
                updatedAt = 1L,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), categoryTerms.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), categoryTerms.uploadedUserIds)
            assertTrue(categoryTerms.changes.value.none { it.isPending })
        }

    @Test
    fun `GIVEN a local profile WHEN syncing THEN the keys of the people in the groups of the user come down`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.memberKeysDownloadedUserIds)
        }

    @Test
    fun `GIVEN the keys of the members fail to come down WHEN syncing THEN the groups still sync and the sync fails`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            paymentKeys.memberKeysError = noConnection

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.uploadedUserIds)
        }

    @Test
    fun `GIVEN the map fails to go up WHEN syncing THEN the sync fails`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        categoryTerms.pushError = noConnection

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN only a custom color waiting to go up WHEN observing the sync state THEN it is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        customColors.add(teal, updatedAt = 1L)

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.PENDING, state)
    }

    @Test
    fun `GIVEN an online start already synced WHEN a custom color is added THEN pushes the palette`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = customColors.uploadedUserIds.size

        // When
        customColors.add(teal, updatedAt = 1L)
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, customColors.uploadedUserIds.size)
        assertEquals(FakeAccountRepository.ANONYMOUS_USER_ID, customColors.uploadedUserIds.last())
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a Google account with custom colors WHEN signing out THEN erases the palette`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )
        customColors.add(teal, updatedAt = 1L)

        // When
        coordinator.signOut()

        // Then
        assertTrue(customColors.isCleared)
        assertTrue(customColors.colors.value.isEmpty())
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
    fun `GIVEN a sync that failed while the session refreshed WHEN it gets a token again THEN syncs again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
        )
        profile.pullError = IllegalStateException("session refreshing")
        coordinator.start()
        runCurrent()
        profile.pullError = null

        // When
        account.sessionRecoveries.emit(Unit)
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.uploadedUserIds)
    }

    @Test
    fun `GIVEN the last sync went through WHEN the session gets a token again THEN syncs no more`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
        )
        coordinator.start()
        runCurrent()

        // When
        account.sessionRecoveries.emit(Unit)
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.downloadedUserIds)
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
    fun `GIVEN online and the pull fails WHEN syncing THEN reports the failure by its kind only`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = IllegalStateException("server down for Pierre")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf("IllegalStateException"), reportedFailures.map { it.message })
    }

    @Test
    fun `GIVEN offline and the pull fails WHEN syncing THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        profile.pullError = IllegalStateException("no internet")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(emptyList(), reportedFailures)
    }

    @Test
    fun `GIVEN online WHEN a sync succeeds THEN reports nothing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingChanges = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(emptyList(), reportedFailures)
    }

    @Test
    fun `GIVEN online and the pull gets no answer WHEN syncing THEN reports nothing`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = noAnswer

        // When
        coordinator.syncNow()

        // Then
        assertEquals(emptyList(), reportedFailures)
    }

    @Test
    fun `GIVEN online and the pull fails because a request got no answer WHEN syncing THEN reports nothing`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            profile.pullError = IllegalStateException("pull failed", noAnswer)

            // When
            coordinator.syncNow()

            // Then
            assertEquals(emptyList(), reportedFailures)
        }

    @Test
    fun `GIVEN online and the failure holds an id and a name WHEN syncing THEN the report holds neither`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = IllegalStateException(
            "bill-42 of Pierre refused",
            IllegalArgumentException("Pierre owes 312 in group-7"),
        )

        // When
        coordinator.syncNow()

        // Then
        val message = reportedFailures.single().message.orEmpty()
        assertEquals("IllegalStateException ← IllegalArgumentException", message)
        assertFalse("bill-42" in message || "group-7" in message || "Pierre" in message)
    }

    @Test
    fun `GIVEN online WHEN a sync fails THEN the report has no cause`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = IllegalStateException("server down", IllegalArgumentException("refused"))

        // When
        coordinator.syncNow()

        // Then
        val reported = reportedFailures.single()
        assertIs<ReportableFailure>(reported)
        assertNull(reported.cause)
    }

    @Test
    fun `GIVEN a failure already reported WHEN the next sync fails online THEN reports nothing more`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        profile.pullError = IllegalStateException("server down")
        coordinator.syncNow()

        // When
        coordinator.syncNow()

        // Then
        assertEquals(1, reportedFailures.size)
    }

    @Test
    fun `GIVEN a failure already reported WHEN another kind of failure follows online THEN reports nothing more`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            profile.pullError = IllegalStateException("server down")
            coordinator.syncNow()
            profile.pullError = IllegalArgumentException("refused")

            // When
            coordinator.syncNow()

            // Then
            assertEquals(listOf("IllegalStateException"), reportedFailures.map { it.message })
        }

    @Test
    fun `GIVEN a failure reported and then a sync that got through WHEN a sync fails online THEN reports it again`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            profile.pullError = IllegalStateException("server down")
            coordinator.syncNow()
            profile.pullError = null
            coordinator.syncNow()
            profile.pullError = IllegalArgumentException("refused")

            // When
            coordinator.syncNow()

            // Then
            assertEquals(
                listOf("IllegalStateException", "IllegalArgumentException"),
                reportedFailures.map { it.message },
            )
        }

    @Test
    fun `GIVEN a sync that failed offline WHEN a sync fails online THEN reports the online one`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        profile.pullError = IllegalStateException("no internet")
        coordinator.syncNow()
        isOnline.value = true
        profile.pullError = IllegalArgumentException("refused")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf("IllegalArgumentException"), reportedFailures.map { it.message })
    }

    @Test
    fun `GIVEN a sync whose request got no answer WHEN a sync fails online for another reason THEN reports it`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            profile.pullError = noAnswer
            coordinator.syncNow()
            profile.pullError = IllegalArgumentException("refused")

            // When
            coordinator.syncNow()

            // Then
            assertEquals(listOf("IllegalArgumentException"), reportedFailures.map { it.message })
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
    fun `GIVEN a first download pending WHEN the sync with the restored profile succeeds THEN it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN an account awaiting its full sync WHEN a sync succeeds THEN it awaits no more`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        account.awaitsFullSync = true

        // When
        coordinator.syncNow()

        // Then
        assertFalse(account.awaitsFullSync)
    }

    @Test
    fun `GIVEN an account awaiting its full sync WHEN the groups fail to come down THEN it still awaits it`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            account.awaitsFullSync = true
            groups.pullError = IllegalStateException("offline")

            // When
            coordinator.syncNow()

            // Then
            assertTrue(account.awaitsFullSync)
        }

    @Test
    fun `GIVEN a profile WHEN a sync fails THEN the first download knows it`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        groups.pushError = IllegalStateException("refused")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(firstDownload.observeHasSyncFailed().value)
    }

    @Test
    fun `GIVEN a first download pending and the groups fail to come down WHEN syncing THEN it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
        )
        groups.pullError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download pending and the server cannot be reached WHEN syncing THEN it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
        )
        account.registerError = IllegalStateException("no connection")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download pending and no profile on the device WHEN syncing THEN it stays pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            firstDownloadPending = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertTrue(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download pending with the restored profile WHEN the device is offline THEN it ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            online = false,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download pending before the profile came WHEN the device is offline THEN it stays pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                firstDownloadPending = true,
                online = false,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertTrue(firstDownload.isPending)
        }

    @Test
    fun `GIVEN a first download pending WHEN syncing THEN each group comes whole in server order before the rest`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip, dinner),
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    "groups first download ids",
                    "groups download group trip",
                    "categories download group trip",
                    "bills download group trip",
                    "payments download group trip",
                    "groups download group dinner",
                    "categories download group dinner",
                    "bills download group dinner",
                    "payments download group dinner",
                    "groups download ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                    "groups upload ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                    "bills download ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                    "bills upload ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                    "payments download ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                    "payments upload ${FakeAccountRepository.ANONYMOUS_USER_ID}",
                ),
                groupsBlockCalls(),
            )
        }

    @Test
    fun `GIVEN a first download pending WHEN syncing THEN the groups of the server reach the device`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = listOf(trip, dinner),
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(listOf("trip", "dinner"), groups.downloadedGroupIds)
        assertEquals(listOf("trip", "dinner"), groups.groups.value.map { it.id })
    }

    @Test
    fun `GIVEN a first download pending WHEN syncing THEN counts each group out of those coming and ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = listOf(trip, dinner),
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(
            listOf(
                FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
                FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = 2,
                    isWaitingForInternet = false,
                ),
                FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 2,
                    isWaitingForInternet = false,
                ),
                FirstDownloadProgress(
                    arrivedGroupCount = 2,
                    groupCount = 2,
                    isWaitingForInternet = false,
                ),
                null,
            ),
            firstDownloadProgress,
        )
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download pending WHEN syncing THEN keeps its count through the full download and ends after`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip),
            )

            // When
            coordinator.syncNow()

            // Then
            assertEquals(listOf(true), pendingAtGroupsFullDownload)
            assertFalse(firstDownload.isPending)
        }

    @Test
    fun `GIVEN a first download and no group on the server WHEN syncing THEN never counts and ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = emptyList(),
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
                null,
            ),
            firstDownloadProgress,
        )
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(categories.downloadedGroupIds.isEmpty())
        assertTrue(expenses.downloadedGroupIds.isEmpty())
        assertTrue(settlements.downloadedGroupIds.isEmpty())
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN no first download pending WHEN syncing THEN no group comes down one by one`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            serverGroups = listOf(trip, dinner),
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertFalse("groups first download ids" in calls)
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(categories.downloadedGroupIds.isEmpty())
        assertTrue(expenses.downloadedGroupIds.isEmpty())
        assertTrue(settlements.downloadedGroupIds.isEmpty())
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN a first download done WHEN syncing again THEN no group comes down one by one`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = listOf(trip, dinner),
        )
        coordinator.syncNow()

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("trip", "dinner"), groups.downloadedGroupIds)
        assertEquals(1, calls.count { it == "groups first download ids" })
        assertEquals(
            listOf(FakeAccountRepository.ANONYMOUS_USER_ID, FakeAccountRepository.ANONYMOUS_USER_ID),
            groups.downloadedUserIds,
        )
    }

    @Test
    fun `GIVEN a first download pending and no profile on the device WHEN syncing THEN no group comes down`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                firstDownloadPending = true,
                serverGroups = listOf(trip),
            )

            // When
            coordinator.syncNow()

            // Then
            assertFalse("groups first download ids" in calls)
            assertTrue(groups.downloadedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a first download and no list of groups from the server WHEN syncing THEN the full download runs`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip),
            )
            groups.firstDownloadError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertFalse(firstDownload.isPending)
            assertTrue(groups.downloadedGroupIds.isEmpty())
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), expenses.downloadedUserIds)
        }

    @Test
    fun `GIVEN a first download and the bills of the first group failing WHEN syncing THEN none of that group lands`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip, dinner),
            )
            expenses.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertTrue(groups.downloadedGroupIds.isEmpty())
            assertTrue(categories.downloadedGroupIds.isEmpty())
            assertTrue(expenses.downloadedGroupIds.isEmpty())
            assertTrue(settlements.downloadedGroupIds.isEmpty())
            assertFalse(firstDownload.isPending)
        }

    @Test
    fun `GIVEN a first download and the categories of a group failing WHEN syncing THEN none of it lands`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = listOf(trip, dinner),
        )
        categories.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(categories.downloadedGroupIds.isEmpty())
        assertTrue(expenses.downloadedGroupIds.isEmpty())
        assertTrue(settlements.downloadedGroupIds.isEmpty())
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download and the payments of a group failing WHEN syncing THEN that group never counts`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip, dinner),
            )
            settlements.pullError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(
                listOf(
                    FirstDownloadProgress(
                        arrivedGroupCount = 0,
                        groupCount = null,
                        isWaitingForInternet = false,
                    ),
                    FirstDownloadProgress(
                        arrivedGroupCount = 0,
                        groupCount = 2,
                        isWaitingForInternet = false,
                    ),
                    null,
                ),
                firstDownloadProgress,
            )
            assertTrue(groups.downloadedGroupIds.isEmpty())
            assertTrue(expenses.downloadedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a first download and the second group failing WHEN syncing THEN the first one counted before the end`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                firstDownloadPending = true,
                serverGroups = listOf(trip, dinner),
                failingGroupDownloadId = "dinner",
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    FirstDownloadProgress(
                        arrivedGroupCount = 0,
                        groupCount = null,
                        isWaitingForInternet = false,
                    ),
                    FirstDownloadProgress(
                        arrivedGroupCount = 0,
                        groupCount = 2,
                        isWaitingForInternet = false,
                    ),
                    FirstDownloadProgress(
                        arrivedGroupCount = 1,
                        groupCount = 2,
                        isWaitingForInternet = false,
                    ),
                    null,
                ),
                firstDownloadProgress,
            )
            assertEquals(listOf("trip"), categories.downloadedGroupIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
            assertFalse(firstDownload.isPending)
        }

    @Test
    fun `GIVEN a first download and a group failing WHEN syncing THEN the rest of the sync still runs`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            firstDownloadPending = true,
            serverGroups = listOf(trip),
            failingGroupDownloadId = "trip",
            logsCommentsAndActivity = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertTrue("activity download ${FakeAccountRepository.ANONYMOUS_USER_ID}" in calls)
        assertTrue("comments download ${FakeAccountRepository.ANONYMOUS_USER_ID}" in calls)
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
    fun `GIVEN a sync in flight WHEN changing the invite link THEN waits for it before rotating the code`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pullHangs = true,
            storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
        )
        backgroundScope.launch { coordinator.syncNow() }
        runCurrent()
        backgroundScope.launch { coordinator.rotateInviteCode("dinner") }
        runCurrent()
        val rotatedDuringSync = groups.rotatedInviteCodeGroupIds.toList()

        // When
        pullGate.complete(Unit)
        runCurrent()

        // Then
        assertTrue(rotatedDuringSync.isEmpty())
        assertEquals(listOf("dinner"), groups.rotatedInviteCodeGroupIds)
        val rotation = calls.indexOf("groups rotate invite code dinner")
        assertTrue(rotation > calls.indexOfLast { it.startsWith("groups upload") })
        assertEquals(
            "newCode1",
            groups.groups.value
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a link change waiting on a sync WHEN the screen that asked leaves THEN still rotates the code`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pullHangs = true,
                storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
            )
            backgroundScope.launch { coordinator.syncNow() }
            runCurrent()
            val caller = backgroundScope.launch { coordinator.rotateInviteCode("dinner") }
            runCurrent()

            // When
            caller.cancel()
            pullGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf("dinner"), groups.rotatedInviteCodeGroupIds)
            assertEquals(
                "newCode1",
                groups.groups.value
                    .single()
                    .inviteCode,
            )
        }

    @Test
    fun `GIVEN the server fails the link change WHEN changing it THEN fails and syncs to bring the code it holds`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
            )
            groups.rotateInviteCodeError = IllegalStateException("network")

            // When
            val result = coordinator.rotateInviteCode("dinner")
            runCurrent()

            // Then
            assertTrue(result.isFailure)
            assertEquals(
                "oldCode1",
                groups.groups.value
                    .single()
                    .inviteCode,
            )
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        }

    @Test
    fun `GIVEN someone not the creator WHEN changing the link THEN the refusal comes back without a sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
        )
        groups.rotateInviteCodeError = NotGroupCreatorException()

        // When
        val result = coordinator.rotateInviteCode("dinner")
        runCurrent()

        // Then
        assertTrue(result.exceptionOrNull() is NotGroupCreatorException)
        assertEquals(
            "oldCode1",
            groups.groups.value
                .single()
                .inviteCode,
        )
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the group still waits to go up WHEN changing the link THEN uploads it before rotating the code`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedGroups = listOf(
                    dinner.copy(
                        inviteCode = "AbCd1234",
                        isPending = true,
                    ),
                ),
            )
            groups.hasPendingChanges.value = true
            groups.groupIdsNotOnServer += "dinner"

            // When
            val result = coordinator.rotateInviteCode("dinner")
            runCurrent()

            // Then
            assertTrue(result.isSuccess)
            val upload = calls.indexOf("groups upload ${FakeAccountRepository.ANONYMOUS_USER_ID}")
            assertTrue(upload >= 0)
            assertTrue(calls.indexOf("groups rotate invite code dinner") > upload)
        }

    @Test
    fun `GIVEN a new group whose sync fails after it went up WHEN changing the link THEN still rotates the code`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedGroups = listOf(
                    dinner.copy(
                        inviteCode = "AbCd1234",
                        isPending = true,
                    ),
                ),
            )
            groups.hasPendingChanges.value = true
            groups.groupIdsNotOnServer += "dinner"
            expenses.pushError = IllegalStateException("a bill the server refuses")

            // When
            val result = coordinator.rotateInviteCode("dinner")
            runCurrent()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("dinner"), groups.rotatedInviteCodeGroupIds)
        }

    @Test
    fun `GIVEN a new group whose upload fails WHEN changing the link THEN fails without rotating`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedGroups = listOf(
                dinner.copy(
                    inviteCode = "AbCd1234",
                    isPending = true,
                ),
            ),
        )
        groups.hasPendingChanges.value = true
        groups.groupIdsNotOnServer += "dinner"
        groups.pushError = IllegalStateException("network")

        // When
        val result = coordinator.rotateInviteCode("dinner")
        runCurrent()

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.rotatedInviteCodeGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN a group on the server and other changes waiting WHEN changing the link THEN rotates without a sync`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
            )
            groups.hasPendingChanges.value = true
            groups.pushError = IllegalStateException("a bill the server refuses")

            // When
            val result = coordinator.rotateInviteCode("dinner")
            runCurrent()

            // Then
            assertTrue(result.isSuccess)
            assertTrue(groups.uploadedUserIds.isEmpty())
            assertEquals(
                "newCode1",
                groups.groups.value
                    .single()
                    .inviteCode,
            )
        }

    @Test
    fun `GIVEN the server changes the link WHEN changing it THEN succeeds without another sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedGroups = listOf(dinner.copy(inviteCode = "oldCode1")),
        )

        // When
        val result = coordinator.rotateInviteCode("dinner")
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            "newCode1",
            groups.groups.value
                .single()
                .inviteCode,
        )
        assertTrue(groups.downloadedUserIds.isEmpty())
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
    fun `GIVEN a local profile WHEN syncing THEN syncs the app invite under the synced user id`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), appInvite.syncedUserIds)
    }

    @Test
    fun `GIVEN the app invite fails to sync WHEN syncing THEN the sync still succeeds with the preferences`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPreferences = true,
            )
            appInvite.syncError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), notificationPreferences.uploadedUserIds)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a dismissal of the invite not sent WHEN syncing THEN the sync state stays synced`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        appInvite.hasPendingChanges.value = true
        appInvite.syncError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
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
                type = NotificationType.PEOPLE_AND_GROUPS,
                channel = NotificationChannel.PUSH,
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
            type = NotificationType.BILLS,
            channel = NotificationChannel.PUSH,
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
                type = NotificationType.PEOPLE_AND_GROUPS,
                channel = NotificationChannel.PUSH,
                isEnabled = true,
            )
            runCurrent()
            notificationPreferences.pushError = null
            val uploadsBefore = notificationPreferences.uploadedUserIds.size

            // When
            notificationPreferences.updatePreference(
                type = NotificationType.BALANCE_REMINDER,
                channel = NotificationChannel.PUSH,
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
            type = NotificationType.PEOPLE_AND_GROUPS,
            channel = NotificationChannel.PUSH,
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
            type = NotificationType.PEOPLE_AND_GROUPS,
            channel = NotificationChannel.PUSH,
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
            type = NotificationType.PEOPLE_AND_GROUPS,
            channel = NotificationChannel.PUSH,
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
    fun `GIVEN a Google account WHEN syncing THEN pulls and pushes the pins under its user id`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pins.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pins.uploadedUserIds)
    }

    @Test
    fun `GIVEN a group pinned offline WHEN observing the sync state THEN it is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        pins.updatePinned(
            groupId = "floripa",
            isPinned = true,
            updatedAt = 1L,
        )
        runCurrent()

        // When
        val state = coordinator.observeState().first()

        // Then
        assertEquals(SyncState.PENDING, state)
    }

    @Test
    fun `GIVEN the pins fail to come down WHEN syncing THEN fails and pushes no pin`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        pins.downloadError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(emptyList(), pins.uploadedUserIds)
    }

    @Test
    fun `GIVEN the pins fail to go up WHEN syncing THEN still pulls and pushes the bills and the payments`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        pins.uploadError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), expenses.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), settlements.uploadedUserIds)
    }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN neither pulls nor pushes the pins`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        groups.pullError = IllegalStateException("offline")

        // When
        coordinator.syncNow()

        // Then
        assertEquals(emptyList(), pins.downloadedUserIds)
        assertEquals(emptyList(), pins.uploadedUserIds)
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
            calls.filterNot { it.startsWith("keys ") || it.startsWith("preferences ") || it.startsWith("payments ") },
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

    @Test
    fun `GIVEN a pending payment WHEN syncing THEN pulls and pushes the payments right after the bills`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingGroups = true,
            pendingBills = true,
            pendingPayments = true,
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
                "payments download $userId",
                "payments upload $userId",
            ),
            calls.filterNot { it.startsWith("keys ") || it.startsWith("preferences ") },
        )
        assertFalse(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a Google account WHEN syncing THEN pulls and pushes the payments under its user id`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingPayments = true,
        )

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), settlements.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), settlements.uploadedUserIds)
    }

    @Test
    fun `GIVEN no profile and a pending payment WHEN syncing THEN neither pulls nor pushes the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            pendingPayments = true,
        )

        // When
        coordinator.syncNow()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the profile fails to come down WHEN syncing THEN does not touch the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPayments = true,
        )
        profile.pullError = IllegalStateException("server down")

        // When
        coordinator.syncNow()

        // Then
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN neither pulls nor pushes the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPayments = true,
        )
        groups.pullError = IllegalStateException("relation groups does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the bills fail to come down WHEN syncing THEN neither pulls nor pushes the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPayments = true,
        )
        expenses.pullError = IllegalStateException("relation expenses does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the bills push fails WHEN syncing THEN neither pulls nor pushes the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
            pendingPayments = true,
        )
        expenses.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN the payments fail to come down WHEN syncing THEN fails pushes none and the state is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingPayments = true,
        )
        settlements.pullError = IllegalStateException("relation settlements does not exist")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), settlements.downloadedUserIds)
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a pending payment and its push fails WHEN syncing THEN fails keeps it pending and is failed`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPayments = true,
            )
            settlements.pushError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), settlements.uploadedUserIds)
            assertTrue(settlements.hasPendingChanges.value)
            assertEquals(SyncState.FAILED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN the payments fail to come down WHEN syncing THEN still syncs the keys and the preferences`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedKeys = listOf(pixKey),
            pendingKeys = true,
            pendingPreferences = true,
        )
        settlements.pullError = IllegalStateException("relation settlements does not exist")

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
    fun `GIVEN an anonymous user whose payments fail to come down WHEN syncing THEN still issues the recovery code`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            settlements.pullError = IllegalStateException("relation settlements does not exist")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(1, account.recoveryCodeCreations)
        }

    @Test
    fun `GIVEN a sync that failed on the payments WHEN a retry succeeds THEN pushes them and the state is synced`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPayments = true,
            )
            settlements.pushError = IllegalStateException("server down")
            coordinator.syncNow()
            settlements.pushError = null

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertFalse(settlements.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN an anonymous user WHEN switching to another Google user THEN syncs the payments under it`() = runTest {
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
        assertEquals(listOf(OTHER_USER_ID), settlements.downloadedUserIds)
        assertEquals(listOf(OTHER_USER_ID), settlements.uploadedUserIds)
    }

    @Test
    fun `GIVEN nothing else pending and a payment recorded offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            settlements.hasPendingChanges.value = true
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN only a pending payment online before any sync WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingPayments = true,
            )

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN a pending bill and a pending payment WHEN the sync pushes both THEN the sync state is synced`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingBills = true,
                pendingPayments = true,
            )

            // When
            coordinator.syncNow()

            // Then
            assertFalse(expenses.hasPendingChanges.value)
            assertFalse(settlements.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN the bills went up and the payments push failed WHEN going offline THEN the state is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingBills = true,
                pendingPayments = true,
            )
            settlements.pushError = IllegalStateException("no answer")
            coordinator.syncNow()

            // When
            isOnline.value = false

            // Then
            assertFalse(expenses.hasPendingChanges.value)
            assertEquals(SyncState.PENDING, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN an online start already synced WHEN only a payment becomes pending THEN pushes the payments`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            val uploadsBefore = settlements.uploadedUserIds.size

            // When
            settlements.hasPendingChanges.value = true
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, settlements.uploadedUserIds.size)
            assertFalse(settlements.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a payments push that failed online WHEN another payment is recorded THEN pushes the payments again`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            settlements.pushError = IllegalStateException("no answer")
            settlements.hasPendingChanges.value = true
            settlements.pendingVersions.value = listOf(1_000L)
            runCurrent()
            settlements.pushError = null
            val uploadsBefore = settlements.uploadedUserIds.size

            // When
            settlements.pendingVersions.value = listOf(1_000L, 2_000L)
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, settlements.uploadedUserIds.size)
            assertFalse(settlements.hasPendingChanges.value)
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a bills push that failed online WHEN a payment is recorded THEN syncs again and pushes both`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            expenses.pushError = IllegalStateException("no answer")
            expenses.hasPendingChanges.value = true
            expenses.pendingVersions.value = listOf(1_000L)
            runCurrent()
            expenses.pushError = null
            val billUploadsBefore = expenses.uploadedUserIds.size
            val paymentUploadsBefore = settlements.uploadedUserIds.size

            // When
            settlements.hasPendingChanges.value = true
            settlements.pendingVersions.value = listOf(2_000L)
            runCurrent()

            // Then
            assertEquals(billUploadsBefore + 1, expenses.uploadedUserIds.size)
            assertEquals(paymentUploadsBefore + 1, settlements.uploadedUserIds.size)
            assertFalse(expenses.hasPendingChanges.value)
            assertFalse(settlements.hasPendingChanges.value)
        }

    @Test
    fun `GIVEN a payments push that failed online WHEN the same payment versions come again THEN does not push`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            settlements.pushError = IllegalStateException("no answer")
            settlements.hasPendingChanges.value = true
            settlements.pendingVersions.value = listOf(1_000L)
            runCurrent()
            settlements.pushError = null
            val uploadsBefore = settlements.uploadedUserIds.size
            val sameVersions = listOf(1_000L)

            // When
            paymentVersionsAgain.tryEmit(sameVersions)
            runCurrent()

            // Then
            assertEquals(uploadsBefore, settlements.uploadedUserIds.size)
            assertTrue(settlements.hasPendingChanges.value)
        }

    @Test
    fun `GIVEN a payment recorded offline WHEN the device comes online THEN pushes the payments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        settlements.hasPendingChanges.value = true
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), settlements.uploadedUserIds)
        assertFalse(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN a profile offline WHEN a payment is recorded THEN does not sync`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()

        // When
        settlements.hasPendingChanges.value = true
        settlements.pendingVersions.value = listOf(1_000L)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
        assertTrue(settlements.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN no profile online WHEN a payment is recorded THEN does not sync`() = runTest {
        // Given
        prepareScenario(storedProfile = null)
        coordinator.start()
        runCurrent()

        // When
        settlements.hasPendingChanges.value = true
        settlements.pendingVersions.value = listOf(1_000L)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(settlements.downloadedUserIds.isEmpty())
        assertTrue(settlements.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN only a payment queued online WHEN signing out once synced THEN pushes it before ending the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingPayments = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId), settlements.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("payments upload $userId"))
        }

    @Test
    fun `GIVEN the payments push fails WHEN signing out once synced THEN keeps the session and the schedule`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedGroups = listOf(trip),
                pendingPayments = true,
            )
            settlements.pushError = IllegalStateException("server down")

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(0, groups.clearCount)
            assertEquals(pierre, profile.profile.value)
            assertTrue(settlements.hasPendingChanges.value)
            assertTrue(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a local profile WHEN syncing THEN sends the activity up and brings it down under the user id`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.uploadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
        }

    @Test
    fun `GIVEN the activity fails to come down WHEN syncing THEN fails and the sync state is failed`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        activity.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the activity fails to go up WHEN syncing THEN fails and brings no activity down`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        activity.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(activity.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the groups fail to sync WHEN syncing THEN still syncs the activity`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        groups.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN a Google account WHEN signing out THEN erases the activity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )

        // When
        coordinator.signOut()

        // Then
        assertEquals(1, activity.clearCalls)
    }

    @Test
    fun `GIVEN a Google account WHEN deleting the account THEN erases the activity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )

        // When
        coordinator.deleteAccount()

        // Then
        assertEquals(1, activity.clearCalls)
    }

    @Test
    fun `GIVEN a Google account and a pending comment WHEN syncing THEN pulls and pushes it under its user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedComments = listOf(comment(id = "uber")),
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), billComments.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), billComments.uploadedUserIds)
            assertTrue(billComments.comments.value.none { it.isPending })
        }

    @Test
    fun `GIVEN a pending comment WHEN syncing THEN syncs the comments after the payments and before the activity`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedComments = listOf(comment(id = "uber")),
                logsCommentsAndActivity = true,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            val userId = FakeAccountRepository.ANONYMOUS_USER_ID
            assertEquals(
                listOf(
                    "groups download $userId",
                    "groups upload $userId",
                    "bills download $userId",
                    "bills upload $userId",
                    "payments download $userId",
                    "payments upload $userId",
                    "comments download $userId",
                    "comments upload $userId",
                    "activity upload $userId",
                    "activity download $userId",
                ),
                calls.filterNot {
                    it.startsWith("profile ") || it.startsWith("keys ") || it.startsWith("preferences ")
                },
            )
        }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN neither pulls nor pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        groups.pullError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billComments.downloadedUserIds.isEmpty())
        assertTrue(billComments.uploadedUserIds.isEmpty())
        assertTrue(
            billComments.comments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the categories fail to go up WHEN syncing THEN neither pulls nor pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        categories.pushError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billComments.downloadedUserIds.isEmpty())
        assertTrue(billComments.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the bills fail to go up WHEN syncing THEN neither pulls nor pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
            storedComments = listOf(comment(id = "uber")),
        )
        expenses.pushError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billComments.downloadedUserIds.isEmpty())
        assertTrue(billComments.uploadedUserIds.isEmpty())
        assertTrue(
            billComments.comments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the payments fail to come down WHEN syncing THEN neither pulls nor pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        settlements.pullError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billComments.downloadedUserIds.isEmpty())
        assertTrue(billComments.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the pins fail to go up WHEN syncing THEN still pulls and pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        pins.uploadError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billComments.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billComments.uploadedUserIds)
        assertTrue(billComments.comments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN the comments fail to come down WHEN syncing THEN pushes none and still syncs the activity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        billComments.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billComments.uploadedUserIds.isEmpty())
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.uploadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN the comments fail to go up WHEN syncing THEN fails keeps the comment pending and is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        billComments.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(
            billComments.comments.value
                .single()
                .isPending,
        )
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the comments fail to go up WHEN syncing THEN still syncs the money and the activity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
            pendingPayments = true,
            storedComments = listOf(comment(id = "uber")),
        )
        billComments.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertFalse(expenses.hasPendingChanges.value)
        assertFalse(settlements.hasPendingChanges.value)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.uploadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN a comments push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
        )
        billComments.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN nothing else pending and a comment written offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            billComments.addComment(comment(id = "uber"))
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
        }

    @Test
    fun `GIVEN an online start already synced WHEN a comment is written THEN pushes the comments`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = billComments.uploadedUserIds.size

        // When
        billComments.addComment(comment(id = "uber"))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, billComments.uploadedUserIds.size)
        assertTrue(billComments.comments.value.none { it.isPending })
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a comment written offline WHEN the device comes online THEN pushes the comments`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        billComments.addComment(comment(id = "uber"))
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billComments.uploadedUserIds)
        assertTrue(billComments.comments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN a comments push that failed online WHEN another comment is written THEN pushes the comments again`() =
        runTest {
            // Given
            prepareScenario(storedProfile = pierre)
            coordinator.start()
            runCurrent()
            billComments.pushError = IllegalStateException("no answer")
            billComments.addComment(comment(id = "uber"))
            runCurrent()
            billComments.pushError = null
            val uploadsBefore = billComments.uploadedUserIds.size

            // When
            billComments.addComment(comment(id = "pizza"))
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, billComments.uploadedUserIds.size)
            assertTrue(billComments.comments.value.none { it.isPending })
            assertEquals(SyncState.SYNCED, coordinator.observeState().first())
        }

    @Test
    fun `GIVEN a bills push that failed online WHEN a comment is written THEN syncs again and pushes both`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        expenses.pushError = IllegalStateException("no answer")
        expenses.hasPendingChanges.value = true
        expenses.pendingVersions.value = listOf(1_000L)
        runCurrent()
        expenses.pushError = null
        val billUploadsBefore = expenses.uploadedUserIds.size
        val commentUploadsBefore = billComments.uploadedUserIds.size

        // When
        billComments.addComment(comment(id = "uber"))
        runCurrent()

        // Then
        assertEquals(billUploadsBefore + 1, expenses.uploadedUserIds.size)
        assertEquals(commentUploadsBefore + 1, billComments.uploadedUserIds.size)
        assertFalse(expenses.hasPendingChanges.value)
        assertTrue(billComments.comments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN a comment written while the comments push is in flight WHEN retrying THEN pushes it in another round`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedComments = listOf(comment(id = "uber")),
                commentQueuedDuringFirstPush = true,
            )

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertEquals(2, billComments.uploadedUserIds.size)
            assertEquals(2, billComments.comments.value.size)
            assertTrue(billComments.comments.value.none { it.isPending })
        }

    @Test
    fun `GIVEN only a comment queued online WHEN signing out once synced THEN pushes it before ending the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedComments = listOf(comment(id = "uber")),
                logsCommentsAndActivity = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId), billComments.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("comments upload $userId"))
        }

    @Test
    fun `GIVEN the comments push fails WHEN signing out once synced THEN keeps the session and the schedule`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedComments = listOf(comment(id = "uber")),
            )
            billComments.pushError = IllegalStateException("server down")

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(google, account.status)
            assertEquals(pierre, profile.profile.value)
            assertTrue(
                billComments.comments.value
                    .single()
                    .isPending,
            )
            assertTrue(account.signOutWhenSynced)
        }

    @Test
    fun `GIVEN a comment written while the push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedComments = listOf(comment(id = "uber")),
                commentQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(2, billComments.uploadedUserIds.size)
            assertTrue(billComments.comments.value.none { it.isPending })
            assertEquals(1, account.signOutCount)
        }

    @Test
    fun `GIVEN an anonymous user and a pending photo WHEN syncing THEN pulls and pushes it under its user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                storedAttachments = listOf(photo(id = "receipt")),
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.uploadedUserIds)
            assertTrue(billAttachments.attachments.value.none { it.isPending })
        }

    @Test
    fun `GIVEN a Google account and a pending photo WHEN syncing THEN pulls and pushes it under its user id`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedAttachments = listOf(photo(id = "receipt")),
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), billAttachments.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), billAttachments.uploadedUserIds)
            assertTrue(billAttachments.attachments.value.none { it.isPending })
        }

    @Test
    fun `GIVEN a pending photo WHEN syncing THEN syncs the photos after the comments and before the activity`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedAttachments = listOf(photo(id = "receipt")),
                logsCommentsAndActivity = true,
                logsAttachments = true,
            )

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isSuccess)
            val userId = FakeAccountRepository.ANONYMOUS_USER_ID
            assertEquals(
                listOf(
                    "groups download $userId",
                    "groups upload $userId",
                    "bills download $userId",
                    "bills upload $userId",
                    "payments download $userId",
                    "payments upload $userId",
                    "comments download $userId",
                    "comments upload $userId",
                    "attachments download $userId",
                    "attachments upload $userId",
                    "activity upload $userId",
                    "activity download $userId",
                ),
                calls.filterNot {
                    it.startsWith("profile ") || it.startsWith("keys ") || it.startsWith("preferences ")
                },
            )
        }

    @Test
    fun `GIVEN the groups fail to come down WHEN syncing THEN neither pulls nor pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        groups.pullError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billAttachments.downloadedUserIds.isEmpty())
        assertTrue(billAttachments.uploadedUserIds.isEmpty())
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the bills fail to go up WHEN syncing THEN neither pulls nor pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            pendingBills = true,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        expenses.pushError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billAttachments.downloadedUserIds.isEmpty())
        assertTrue(billAttachments.uploadedUserIds.isEmpty())
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the categories fail to go up WHEN syncing THEN neither pulls nor pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        categories.pushError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billAttachments.downloadedUserIds.isEmpty())
        assertTrue(billAttachments.uploadedUserIds.isEmpty())
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the payments fail to come down WHEN syncing THEN neither pulls nor pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        settlements.pullError = IllegalStateException("offline")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billAttachments.downloadedUserIds.isEmpty())
        assertTrue(billAttachments.uploadedUserIds.isEmpty())
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the pins fail to go up WHEN syncing THEN still pulls and pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        pins.uploadError = IllegalStateException("rejected")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.uploadedUserIds)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN the comments fail to come down WHEN syncing THEN still pulls and pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billComments.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.uploadedUserIds)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN the comments fail to go up WHEN syncing THEN still pulls and pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedComments = listOf(comment(id = "uber")),
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billComments.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.uploadedUserIds)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
        assertTrue(
            billComments.comments.value
                .single()
                .isPending,
        )
    }

    @Test
    fun `GIVEN the photos fail to come down WHEN syncing THEN pushes none and still syncs the activity`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billAttachments.pullError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(billAttachments.uploadedUserIds.isEmpty())
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.uploadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN the photos fail to go up WHEN syncing THEN keeps the photo pending and the sync is failed`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billAttachments.pushError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
        assertEquals(SyncState.FAILED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN the photos fail to go up WHEN syncing THEN still syncs the money the comments and the activity`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                pendingBills = true,
                pendingPayments = true,
                storedComments = listOf(comment(id = "uber")),
                storedAttachments = listOf(photo(id = "receipt")),
            )
            billAttachments.pushError = IllegalStateException("server down")

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertFalse(expenses.hasPendingChanges.value)
            assertFalse(settlements.hasPendingChanges.value)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billComments.uploadedUserIds)
            assertTrue(billComments.comments.value.none { it.isPending })
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.uploadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
        }

    @Test
    fun `GIVEN a photos push that failed WHEN the device goes offline THEN the sync state is pending`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billAttachments.pushError = IllegalStateException("no answer")
        coordinator.syncNow()

        // When
        isOnline.value = false

        // Then
        assertEquals(SyncState.PENDING, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN nothing else pending and a photo added offline WHEN observing the sync state THEN it is pending`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
            )
            coordinator.start()
            runCurrent()
            billAttachments.addAttachment(photo(id = "receipt"), jpeg = byteArrayOf(1))
            runCurrent()

            // When
            val state = coordinator.observeState().first()

            // Then
            assertEquals(SyncState.PENDING, state)
            assertTrue(billAttachments.uploadedUserIds.isEmpty())
        }

    @Test
    fun `GIVEN an online start already synced WHEN a photo is added THEN pushes the photos`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        val uploadsBefore = billAttachments.uploadedUserIds.size

        // When
        billAttachments.addAttachment(photo(id = "receipt"), jpeg = byteArrayOf(1))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, billAttachments.uploadedUserIds.size)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a photo added offline WHEN the device comes online THEN pushes the photos`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            online = false,
        )
        coordinator.start()
        runCurrent()
        billAttachments.addAttachment(photo(id = "receipt"), jpeg = byteArrayOf(1))
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.uploadedUserIds)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN a photos push that failed online WHEN another photo is added THEN pushes the photos again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        billAttachments.pushError = IllegalStateException("no answer")
        billAttachments.addAttachment(photo(id = "receipt"), jpeg = byteArrayOf(1))
        runCurrent()
        billAttachments.pushError = null
        val uploadsBefore = billAttachments.uploadedUserIds.size

        // When
        billAttachments.addAttachment(photo(id = "table"), jpeg = byteArrayOf(1))
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, billAttachments.uploadedUserIds.size)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a photos push that failed online WHEN the photo is taken off THEN pushes the photos again`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        billAttachments.pushError = IllegalStateException("no answer")
        billAttachments.addAttachment(
            photo(
                id = "receipt",
                updatedAt = 1L,
            ),
            jpeg = byteArrayOf(1),
        )
        runCurrent()
        billAttachments.pushError = null
        val uploadsBefore = billAttachments.uploadedUserIds.size

        // When
        // Why: the fake drops a removed photo, while the real repository keeps it pending, stamped anew.
        billAttachments.attachments.update { list -> list.map { it.copy(updatedAt = 2L) } }
        runCurrent()

        // Then
        assertEquals(uploadsBefore + 1, billAttachments.uploadedUserIds.size)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN a bills push that failed online WHEN a photo is added THEN syncs again and pushes both`() = runTest {
        // Given
        prepareScenario(storedProfile = pierre)
        coordinator.start()
        runCurrent()
        expenses.pushError = IllegalStateException("no answer")
        expenses.hasPendingChanges.value = true
        expenses.pendingVersions.value = listOf(1_000L)
        runCurrent()
        expenses.pushError = null
        val billUploadsBefore = expenses.uploadedUserIds.size
        val photoUploadsBefore = billAttachments.uploadedUserIds.size

        // When
        billAttachments.addAttachment(photo(id = "receipt"), jpeg = byteArrayOf(1))
        runCurrent()

        // Then
        assertEquals(billUploadsBefore + 1, expenses.uploadedUserIds.size)
        assertEquals(photoUploadsBefore + 1, billAttachments.uploadedUserIds.size)
        assertFalse(expenses.hasPendingChanges.value)
        assertTrue(billAttachments.attachments.value.none { it.isPending })
    }

    @Test
    fun `GIVEN a photo added while the photos push is in flight WHEN retrying THEN pushes it in another round`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                storedAttachments = listOf(photo(id = "receipt")),
                attachmentQueuedDuringFirstPush = true,
            )

            // When
            coordinator.retry()
            runCurrent()

            // Then
            assertEquals(2, billAttachments.uploadedUserIds.size)
            assertEquals(2, billAttachments.attachments.value.size)
            assertTrue(billAttachments.attachments.value.none { it.isPending })
        }

    @Test
    fun `GIVEN only a photo queued online WHEN signing out once synced THEN pushes it before ending the session`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedAttachments = listOf(photo(id = "receipt")),
                logsAttachments = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            val userId = FakeAccountRepository.CONNECTED_USER_ID
            assertEquals(listOf(userId), billAttachments.uploadedUserIds)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(calls.indexOf("account sign out") > calls.lastIndexOf("attachments upload $userId"))
        }

    @Test
    fun `GIVEN the photos push fails WHEN signing out once synced THEN keeps the session and the schedule`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedAttachments = listOf(photo(id = "receipt")),
        )
        billAttachments.pushError = IllegalStateException("server down")

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(0, account.signOutCount)
        assertEquals(google, account.status)
        assertEquals(pierre, profile.profile.value)
        assertTrue(
            billAttachments.attachments.value
                .single()
                .isPending,
        )
        assertTrue(account.signOutWhenSynced)
    }

    @Test
    fun `GIVEN a photo added while the push is in flight WHEN signing out once synced THEN pushes it before`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedAttachments = listOf(photo(id = "receipt")),
                attachmentQueuedDuringFirstPush = true,
            )

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(2, billAttachments.uploadedUserIds.size)
            assertTrue(billAttachments.attachments.value.none { it.isPending })
            assertEquals(1, account.signOutCount)
        }

    @Test
    fun `GIVEN a push token and an anonymous account without a profile WHEN starting THEN registers the device`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                status = anonymous,
                storedPushDevice = androidDevice,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a push token and a Google account online WHEN starting THEN registers the device for the Google user`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPushDevice = androidDevice,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a push token and no account WHEN starting online THEN unregisters it instead`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            storedPushDevice = androidDevice,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertTrue(pushDevices.registeredUserIds.isEmpty())
        assertEquals(1, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN a push token and an account WHEN the push switch is turned off THEN unregisters the device`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()

        // When
        pushDevices.isEnabled.value = false
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        assertEquals(1, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN the push switch turned off WHEN the unregister call fails THEN tries again later`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()
        pushDevices.unregisterError = noConnection
        pushDevices.isEnabled.value = false
        runCurrent()
        pushDevices.unregisterError = null

        // When
        advanceTimeBy(30.seconds)
        runCurrent()

        // Then
        assertEquals(2, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN the push switch turned off WHEN the unregister call keeps failing THEN stops after four tries`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPushDevice = androidDevice,
            )
            coordinator.start()
            runCurrent()
            pushDevices.unregisterError = noConnection

            // When
            pushDevices.isEnabled.value = false
            advanceTimeBy(1.hours)
            runCurrent()

            // Then
            assertEquals(4, pushDevices.unregisterCount)
        }

    @Test
    fun `GIVEN the push switch off WHEN it is turned back on THEN registers the device again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        pushDevices.isEnabled.value = false
        coordinator.start()
        runCurrent()

        // When
        pushDevices.isEnabled.value = true
        runCurrent()

        // Then
        assertEquals(1, pushDevices.unregisterCount)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
    }

    @Test
    fun `GIVEN a push token and no account WHEN starting offline THEN touches nothing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            online = false,
            storedPushDevice = androidDevice,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertTrue(pushDevices.registeredUserIds.isEmpty())
        assertEquals(0, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN a push token and an account offline WHEN starting THEN registers nothing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            online = false,
            storedPushDevice = androidDevice,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertTrue(pushDevices.registeredUserIds.isEmpty())
    }

    @Test
    fun `GIVEN an account online and no push token yet WHEN starting THEN registers nothing`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertTrue(pushDevices.registeredUserIds.isEmpty())
    }

    @Test
    fun `GIVEN an account online and no push token yet WHEN the platform hands the token THEN registers the device`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
            )
            coordinator.start()
            runCurrent()

            // When
            pushDevices.setDevice(androidDevice)
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a push token offline WHEN the device comes online THEN registers the device`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            online = false,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
    }

    @Test
    fun `GIVEN a registered device WHEN the push token changes THEN registers the new token`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()

        // When
        pushDevices.setDevice(androidDevice.copy(token = "fcm-token-renewed"))
        runCurrent()

        // Then
        assertEquals(
            listOf(FakeAccountRepository.CONNECTED_USER_ID, FakeAccountRepository.CONNECTED_USER_ID),
            pushDevices.registeredUserIds,
        )
        assertEquals("fcm-token-renewed", pushDevices.device.value?.token)
    }

    @Test
    fun `GIVEN a registered device WHEN the same push token is handed again THEN does not register again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()

        // When
        pushDevices.setDevice(androidDevice.copy())
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
    }

    @Test
    fun `GIVEN a registered device WHEN a sync runs THEN does not register again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()

        // When
        val result = coordinator.syncNow()
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
    }

    @Test
    fun `GIVEN a device of an anonymous user WHEN switching to another Google user THEN registers it for him`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = anonymous,
                storedPushDevice = androidDevice,
            )
            coordinator.start()
            runCurrent()

            // When
            account.status = AccountStatus.Connected(OTHER_USER_ID, AccountProvider.GOOGLE)
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID, OTHER_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a local only profile and a push token WHEN the first sync makes the account THEN registers it`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                online = false,
                storedPushDevice = androidDevice,
            )
            coordinator.start()
            runCurrent()

            // When
            isOnline.value = true
            runCurrent()

            // Then
            assertEquals(1, account.registrationCount)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a registered device WHEN the device goes offline and comes back online THEN registers it again`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPushDevice = androidDevice,
            )
            coordinator.start()
            runCurrent()
            isOnline.value = false
            runCurrent()

            // When
            isOnline.value = true
            runCurrent()

            // Then
            assertEquals(
                listOf(FakeAccountRepository.CONNECTED_USER_ID, FakeAccountRepository.CONNECTED_USER_ID),
                pushDevices.registeredUserIds,
            )
        }

    @Test
    fun `GIVEN the server refuses the push token WHEN starting THEN the sync still succeeds`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            pendingChanges = true,
            storedPushDevice = androidDevice,
        )
        pushDevices.registerError = noConnection

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
        assertEquals(SyncState.SYNCED, coordinator.observeState().first())
    }

    @Test
    fun `GIVEN a push token the server refused WHEN a change is queued THEN syncs it without registering again`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPushDevice = androidDevice,
            )
            pushDevices.registerError = noConnection
            coordinator.start()
            runCurrent()
            val uploadsBefore = profile.uploadedUserIds.size

            // When
            profile.saveProfile(pierre.copy(avatarColor = AvatarColor.PEACH))
            runCurrent()

            // Then
            assertEquals(uploadsBefore + 1, profile.uploadedUserIds.size)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a push token the server refused WHEN the device comes back online THEN registers it again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        pushDevices.registerError = noConnection
        coordinator.start()
        runCurrent()
        pushDevices.registerError = null
        isOnline.value = false
        runCurrent()

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(
            listOf(FakeAccountRepository.CONNECTED_USER_ID, FakeAccountRepository.CONNECTED_USER_ID),
            pushDevices.registeredUserIds,
        )
    }

    @Test
    fun `GIVEN a registered device WHEN signing out THEN unregisters it once the session ends`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
            logsPushDevice = true,
        )
        coordinator.start()
        runCurrent()

        // When
        coordinator.signOut()
        runCurrent()

        // Then
        assertEquals(1, pushDevices.unregisterCount)
        assertTrue(calls.indexOf("account sign out") < calls.indexOf("push unregister"))
    }

    @Test
    fun `GIVEN a registered device offline WHEN signing out THEN unregisters it only once back online`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        coordinator.start()
        runCurrent()
        isOnline.value = false
        runCurrent()

        // When
        val result = coordinator.signOut()
        runCurrent()
        val unregisteredOffline = pushDevices.unregisterCount
        isOnline.value = true
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertEquals(0, unregisteredOffline)
        assertEquals(1, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN the push token fails to unregister WHEN signing out THEN still ends the session and erases the copy`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedKeys = listOf(pixKey),
                storedPushDevice = androidDevice,
            )
            pushDevices.unregisterError = noConnection
            coordinator.start()
            runCurrent()

            // When
            val result = coordinator.signOut()
            runCurrent()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, pushDevices.unregisterCount)
            assertEquals(1, account.signOutCount)
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertEquals(1, profile.clearCount)
            assertNull(profile.profile.value)
        }

    @Test
    fun `GIVEN the push token failed to unregister WHEN back online THEN tries again`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        pushDevices.unregisterError = noConnection
        coordinator.start()
        runCurrent()
        coordinator.signOut()
        runCurrent()
        pushDevices.unregisterError = null

        // When
        isOnline.value = false
        runCurrent()
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(2, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN the push token unregister never answers WHEN signing out THEN still ends the session`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )
        pushDevices.unregisterHangs = true
        coordinator.start()
        runCurrent()

        // When
        val result = coordinator.signOut()
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, account.signOutCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN a registered device WHEN signing out THEN registers nothing for the device without an account`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                storedPushDevice = androidDevice,
            )
            coordinator.start()
            runCurrent()

            // When
            coordinator.signOut()
            runCurrent()

            // Then
            assertEquals(AccountStatus.LocalOnly, account.status)
            assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), pushDevices.registeredUserIds)
        }

    @Test
    fun `GIVEN a queued change WHEN signing out once synced THEN unregisters the device once the session ends`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = pierre,
                status = google,
                pendingChanges = true,
                storedPushDevice = androidDevice,
                logsPushDevice = true,
            )
            coordinator.start()
            runCurrent()

            // When
            coordinator.signOutWhenSynced()
            runCurrent()

            // Then
            assertEquals(1, pushDevices.unregisterCount)
            assertTrue(calls.indexOf("account sign out") < calls.indexOf("push unregister"))
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN an anonymous account WHEN signing out once synced THEN keeps the device registered`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = anonymous,
            pendingChanges = true,
            storedPushDevice = androidDevice,
        )

        // When
        coordinator.signOutWhenSynced()
        runCurrent()

        // Then
        assertEquals(0, pushDevices.unregisterCount)
    }

    @Test
    fun `GIVEN a registered device WHEN deleting the account THEN does not unregister it`() = runTest {
        // Given
        prepareScenario(
            storedProfile = pierre,
            status = google,
            storedPushDevice = androidDevice,
        )

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, pushDevices.unregisterCount)
    }

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val noConnection = IllegalStateException("no connection")
    private val teal = CustomColor(0x1ABC9C)
    private val noAnswer = HttpRequestException(
        message = "timeout",
        request = HttpRequestBuilder().apply { url("https://example.supabase.co/rest/v1/expenses?id=eq.bill-42") },
    )
    private val androidDevice = PushDevice(
        token = "fcm-token",
        platform = PushPlatform.ANDROID,
        apnsEnvironment = null,
        webP256dh = null,
        webAuth = null,
    )
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

    private val invitesOnly =
        NotificationPreferences(
            push = setOf(NotificationType.PEOPLE_AND_GROUPS),
            inApp = NotificationPreferences.DEFAULT.inApp,
        )

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
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "me",
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun comment(id: String): BillComment = BillComment(
        id = id,
        expenseId = "dinner-bill",
        groupId = "dinner",
        authorUserId = FakeAccountRepository.ANONYMOUS_USER_ID,
        authorMemberId = "me",
        authorName = "Pierre",
        threadId = null,
        replyToName = null,
        body = "Paguei o Uber",
        viaLink = false,
        postedAt = 1L,
        isPending = true,
        editedAt = null,
        isDeleted = false,
        isMine = false,
        likes = emptyList(),
    )

    private fun photo(
        id: String,
        updatedAt: Long = 1L,
    ): BillAttachment = BillAttachment(
        id = id,
        expenseId = "dinner-bill",
        groupId = "dinner",
        kind = BillAttachmentKind.PHOTO,
        width = 1_600,
        height = 1_200,
        addedByUserId = FakeAccountRepository.ANONYMOUS_USER_ID,
        addedByMemberId = "me",
        addedAt = 1L,
        updatedAt = updatedAt,
        isPending = true,
        hasPendingImage = false,
    )

    private fun callsWithoutPreferencesAndGroups(): List<String> = calls.filterNot {
        it.startsWith("preferences ") || it.startsWith("groups ") || it.startsWith("bills ") ||
            it.startsWith("payments ")
    }

    private fun callsWithoutKeysAndGroups(): List<String> = calls.filterNot {
        it.startsWith("keys ") || it.startsWith("groups ") || it.startsWith("bills ") || it.startsWith("payments ")
    }

    private fun callsWithoutKeysAndPreferences(): List<String> = calls.filterNot {
        it.startsWith("keys ") || it.startsWith("preferences ") || it.startsWith("bills ") || it.startsWith("payments ")
    }

    private fun groupsBlockCalls(): List<String> = calls.filter {
        it.startsWith("groups ") || it.startsWith("categories ") || it.startsWith("bills ") ||
            it.startsWith("payments ")
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
        pendingPayments: Boolean = false,
        firstDownloadPending: Boolean = false,
        serverGroups: List<Group> = emptyList(),
        failingGroupDownloadId: String? = null,
        storedComments: List<BillComment> = emptyList(),
        storedAttachments: List<BillAttachment> = emptyList(),
        commentQueuedDuringFirstPush: Boolean = false,
        logsCommentsAndActivity: Boolean = false,
        attachmentQueuedDuringFirstPush: Boolean = false,
        logsAttachments: Boolean = false,
        storedPushDevice: PushDevice? = null,
        logsPushDevice: Boolean = false,
    ) {
        firstDownload = FirstDownload().also { if (firstDownloadPending) it.start() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            firstDownload.observeProgress().collect(firstDownloadProgress::add)
        }
        account = FakeAccountRepository(initialStatus = status).also {
            it.recoveryCodePrompt = prompt
            it.signOutWhenSynced = signOutScheduled
        }
        profile =
            FakeProfileRepository(initialProfile = storedProfile).also { it.hasPendingChanges.value = pendingChanges }
        paymentKeys =
            FakePaymentKeyRepository(initialKeys = storedKeys).also { it.hasPendingChanges.value = pendingKeys }
        appInvite = FakeAppInviteRepository()
        notificationPreferences = FakeNotificationPreferencesRepository(initialPreferences = storedPreferences).also {
            it.hasPendingChanges.value = pendingPreferences
        }
        groups = FakeGroupRepository(initialGroups = storedGroups).also {
            it.hasPendingChanges.value = pendingGroups
            it.serverGroups = serverGroups
        }
        expenses = FakeExpenseRepository().also { it.hasPendingChanges.value = pendingBills }
        settlements = FakeSettlementRepository().also { it.hasPendingChanges.value = pendingPayments }
        activity = FakeActivityRepository()
        categories = FakeCategoryRepository()
        billComments = FakeBillCommentRepository(initialComments = storedComments)
        billAttachments = FakeBillAttachmentRepository(initialAttachments = storedAttachments)
        pushDevices = FakePushDeviceRepository(initialDevice = storedPushDevice)
        // Why: only the cases that assert where the comments go log them, so the others keep their exact logs.
        val commentAndActivityCalls = if (logsCommentsAndActivity) calls else mutableListOf()
        paymentVersionsAgain = MutableSharedFlow(extraBufferCapacity = 1)
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
            appInviteRepository = appInvite,
            notificationPreferencesRepository = RecordingNotificationPreferencesRepository(
                delegate = if (preferenceQueuedDuringFirstPush) {
                    PreferenceDuringFirstPushNotificationPreferencesRepository(notificationPreferences)
                } else {
                    notificationPreferences
                },
                calls = calls,
            ),
            groupRepository = RecordingGroupRepository(
                delegate = when {
                    groupQueuedDuringFirstPush -> GroupDuringFirstPushGroupRepository(groups)

                    failingGroupDownloadId != null -> FailingGroupDownloadGroupRepository(
                        delegate = groups,
                        failingGroupId = failingGroupDownloadId,
                    )

                    else -> groups
                },
                calls = calls,
                beforeDownloadGroups = { pendingAtGroupsFullDownload += firstDownload.isPending },
            ),
            groupPinRepository = FakeGroupPinRepository().also { pins = it },
            groupIncomeRepository = FakeGroupIncomeRepository(),
            expenseRepository = RecordingExpenseRepository(expenses, calls),
            settlementRepository = RecordingSettlementRepository(settlements, calls, paymentVersionsAgain),
            categoryRepository = RecordingCategoryRepository(categories, calls),
            personalCategoryData = PersonalCategoryData(categoryTerms, customColors),
            billCommentRepository = RecordingBillCommentRepository(
                delegate = if (commentQueuedDuringFirstPush) {
                    CommentDuringFirstPushBillCommentRepository(
                        delegate = billComments,
                        queued = comment(id = "written-during-push"),
                    )
                } else {
                    billComments
                },
                calls = commentAndActivityCalls,
            ),
            billAttachmentRepository = RecordingBillAttachmentRepository(
                delegate = if (attachmentQueuedDuringFirstPush) {
                    AttachmentDuringFirstPushBillAttachmentRepository(
                        delegate = billAttachments,
                        queued = photo(id = "added-during-push"),
                    )
                } else {
                    billAttachments
                },
                // Why: only the cases that assert where the photos go log them, so the others keep their exact logs.
                calls = if (logsAttachments) calls else mutableListOf(),
            ),
            activityRepository = RecordingActivityRepository(activity, commentAndActivityCalls),
            hiddenContactRepository = FakeHiddenContactRepository(),
            pushDeviceRepository = RecordingPushDeviceRepository(
                delegate = pushDevices,
                // Why: only the cases that assert when the device leaves log it, so the others keep their exact logs.
                calls = if (logsPushDevice) calls else mutableListOf(),
            ),
            remoteChangeRepository = remoteChanges,
            firstDownload = firstDownload,
            appForeground = appForeground,
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            crashReporter = CrashReporter { reportedFailures += it },
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

// Why: one log across the account, the profile, the keys, the preferences, the groups, the bills and the payments, so
// a test can assert the order of their calls.
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
    private val beforeDownloadGroups: () -> Unit = {},
) : GroupRepository by delegate {
    override suspend fun downloadGroups(userId: String): Result<Unit> {
        calls += "groups download $userId"
        beforeDownloadGroups()
        return delegate.downloadGroups(userId)
    }

    override suspend fun fetchFirstDownloadGroupIds(): Result<List<String>> {
        calls += "groups first download ids"
        return delegate.fetchFirstDownloadGroupIds()
    }

    override suspend fun fetchGroupDownload(
        userId: String,
        groupId: String,
    ): Result<suspend () -> Unit> {
        calls += "groups download group $groupId"
        return delegate.fetchGroupDownload(userId, groupId)
    }

    override suspend fun uploadPendingGroups(userId: String): Result<Unit> {
        calls += "groups upload $userId"
        return delegate.uploadPendingGroups(userId)
    }

    override suspend fun clear() {
        calls += "groups clear"
        delegate.clear()
    }

    override suspend fun rotateInviteCode(groupId: String): Result<Unit> {
        calls += "groups rotate invite code $groupId"
        return delegate.rotateInviteCode(groupId)
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

// Why: one group of a first download that fails to come down, after the ones before it came.
private class FailingGroupDownloadGroupRepository(
    private val delegate: FakeGroupRepository,
    private val failingGroupId: String,
) : GroupRepository by delegate {
    override suspend fun fetchGroupDownload(
        userId: String,
        groupId: String,
    ): Result<suspend () -> Unit> = if (groupId == failingGroupId) {
        Result.failure(IllegalStateException("server down"))
    } else {
        delegate.fetchGroupDownload(userId, groupId)
    }
}

// Why: logs only a group's categories of a first download, so the other cases keep their exact logs.
private class RecordingCategoryRepository(
    private val delegate: CategoryRepository,
    private val calls: MutableList<String>,
) : CategoryRepository by delegate {
    override suspend fun fetchGroupCategories(groupId: String): Result<suspend () -> Unit> {
        calls += "categories download group $groupId"
        return delegate.fetchGroupCategories(groupId)
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

    override suspend fun fetchGroupExpenses(groupId: String): Result<suspend () -> Unit> {
        calls += "bills download group $groupId"
        return delegate.fetchGroupExpenses(groupId)
    }
}

/*
 * Why: also lets a test hand the coordinator the pending payments' versions again, as Room does whenever their table
 * changes, without them being new.
 */
private class RecordingSettlementRepository(
    private val delegate: FakeSettlementRepository,
    private val calls: MutableList<String>,
    private val versionsAgain: Flow<List<Long>>,
) : SettlementRepository by delegate {
    override fun observePendingVersions(): Flow<List<Long>> = merge(delegate.observePendingVersions(), versionsAgain)

    override suspend fun downloadSettlements(userId: String): Result<Unit> {
        calls += "payments download $userId"
        return delegate.downloadSettlements(userId)
    }

    override suspend fun uploadPendingSettlements(userId: String): Result<Unit> {
        calls += "payments upload $userId"
        return delegate.uploadPendingSettlements(userId)
    }

    override suspend fun fetchGroupSettlements(groupId: String): Result<suspend () -> Unit> {
        calls += "payments download group $groupId"
        return delegate.fetchGroupSettlements(groupId)
    }
}

// Why: a comment written while the first comments push is in flight, so one is still pending once it ends.
private class CommentDuringFirstPushBillCommentRepository(
    private val delegate: FakeBillCommentRepository,
    private val queued: BillComment,
) : BillCommentRepository by delegate {
    override suspend fun uploadPending(userId: String): Result<Unit> {
        val result = delegate.uploadPending(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.comments.update { it + queued }
        return result
    }
}

private class RecordingBillCommentRepository(
    private val delegate: BillCommentRepository,
    private val calls: MutableList<String>,
) : BillCommentRepository by delegate {
    override suspend fun download(userId: String): Result<Unit> {
        calls += "comments download $userId"
        return delegate.download(userId)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        calls += "comments upload $userId"
        return delegate.uploadPending(userId)
    }
}

// Why: a photo added while the first photos push is in flight, so one is still pending once it ends.
private class AttachmentDuringFirstPushBillAttachmentRepository(
    private val delegate: FakeBillAttachmentRepository,
    private val queued: BillAttachment,
) : BillAttachmentRepository by delegate {
    override suspend fun uploadPending(userId: String): Result<Unit> {
        val result = delegate.uploadPending(userId)
        if (delegate.uploadedUserIds.size == 1) delegate.attachments.update { it + queued }
        return result
    }
}

private class RecordingBillAttachmentRepository(
    private val delegate: BillAttachmentRepository,
    private val calls: MutableList<String>,
) : BillAttachmentRepository by delegate {
    override suspend fun download(userId: String): Result<Unit> {
        calls += "attachments download $userId"
        return delegate.download(userId)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        calls += "attachments upload $userId"
        return delegate.uploadPending(userId)
    }
}

private class RecordingActivityRepository(
    private val delegate: ActivityRepository,
    private val calls: MutableList<String>,
) : ActivityRepository by delegate {
    override suspend fun uploadPendingActivity(userId: String): Result<Unit> {
        calls += "activity upload $userId"
        return delegate.uploadPendingActivity(userId)
    }

    override suspend fun downloadActivity(userId: String): Result<Unit> {
        calls += "activity download $userId"
        return delegate.downloadActivity(userId)
    }
}

private class RecordingPushDeviceRepository(
    private val delegate: PushDeviceRepository,
    private val calls: MutableList<String>,
) : PushDeviceRepository by delegate {
    override suspend fun register(userId: String): Result<Unit> {
        calls += "push register $userId"
        return delegate.register(userId)
    }

    override suspend fun unregister(): Result<Unit> {
        calls += "push unregister"
        return delegate.unregister()
    }
}

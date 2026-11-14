package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

// Real time (beta feedback 13): what SyncCoordinator does with the other devices' changes it hears.
class SyncCoordinatorRealtimeTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var gatedProfile: GatedDownloadProfileRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var gatedGroups: GatedDownloadGroupRepository
    private lateinit var categories: FakeCategoryRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var billComments: FakeBillCommentRepository
    private lateinit var billAttachments: FakeBillAttachmentRepository
    private lateinit var activity: FakeActivityRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var remoteChanges: FakeRemoteChangeRepository
    private lateinit var appForeground: AppForeground
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var coordinator: SyncCoordinator
    private val syncGate = CompletableDeferred<Unit>()
    private val states = mutableListOf<SyncState>()

    @Test
    fun `GIVEN online with an account and a profile WHEN the app comes to the front THEN listens`() = runTest {
        // Given
        prepareScenario(inFront = false)

        // When
        appForeground.update(isForeground = true)
        runCurrent()

        // Then
        assertEquals(1, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN the app never came to the front WHEN back online THEN never listens`() = runTest {
        // Given
        prepareScenario(
            inFront = false,
            online = false,
        )

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN listening WHEN the app leaves the front THEN stops listening`() = runTest {
        // Given
        prepareScenario()

        // When
        appForeground.update(isForeground = false)
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN the app left the front WHEN it comes back THEN listens again`() = runTest {
        // Given
        prepareScenario()
        appForeground.update(isForeground = false)
        runCurrent()

        // When
        appForeground.update(isForeground = true)
        runCurrent()

        // Then
        assertEquals(1, remoteChanges.listeners)
        assertEquals(2, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN listening WHEN the internet drops THEN stops listening`() = runTest {
        // Given
        prepareScenario()

        // When
        isOnline.value = false
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN offline in front WHEN back online THEN listens`() = runTest {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true
        runCurrent()

        // Then
        assertEquals(1, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN a profile with no account yet WHEN the app comes to the front offline THEN does not listen`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            online = false,
            inFront = false,
        )

        // When
        appForeground.update(isForeground = true)
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN an account with no profile WHEN the app comes to the front THEN does not listen`() = runTest {
        // Given
        prepareScenario(
            storedProfile = null,
            inFront = false,
        )

        // When
        appForeground.update(isForeground = true)
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN listening WHEN the account is gone THEN stops listening`() = runTest {
        // Given
        prepareScenario()

        // When
        account.status = AccountStatus.LocalOnly
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN listening WHEN the profile is erased THEN stops listening`() = runTest {
        // Given
        prepareScenario()

        // When
        profile.profile.value = null
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.listeners)
    }

    @Test
    fun `GIVEN listening as an anonymous user WHEN the session switches to another user THEN listens again`() =
        runTest {
            // Given
            prepareScenario()

            // When
            account.status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
            runCurrent()

            // Then
            assertEquals(1, remoteChanges.listeners)
            assertEquals(2, remoteChanges.joinCount)
        }

    @Test
    fun `GIVEN the session switched to another user WHEN their profile changes elsewhere THEN pulls it as theirs`() =
        runTest {
            // Given
            prepareScenario()
            account.status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
            runCurrent()
            val profileDownloadsBefore = profile.downloadedUserIds.size

            // When
            remoteChanges.changes.emit(RemoteChange.ProfileChanged(FakeAccountRepository.CONNECTED_USER_ID))
            advanceTimeBy(BATCH_WAIT)
            runCurrent()

            // Then
            assertEquals(
                listOf(FakeAccountRepository.CONNECTED_USER_ID),
                profile.downloadedUserIds.drop(profileDownloadsBefore),
            )
            assertEquals(0, groups.memberPhotoDownloads)
        }

    @Test
    fun `GIVEN listening WHEN a bill edited in a group is heard THEN pulls that group and the activity`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
        assertEquals(listOf(FLORIPA), categories.downloadedGroupIds)
        assertEquals(listOf(FLORIPA), expenses.downloadedGroupIds)
        assertEquals(listOf(FLORIPA), settlements.downloadedGroupIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN listening WHEN a bill edited in a group is heard THEN runs no full sync`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(0, account.registrationCount)
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(expenses.downloadedUserIds.isEmpty())
        assertTrue(profile.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN listening WHEN a group is pulled THEN the sync badge never shows syncing`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
        assertFalse(SyncState.SYNCING in states)
    }

    @Test
    fun `GIVEN a change just heard WHEN less than the batch window passes THEN nothing is pulled yet`() = runTest {
        // Given
        prepareScenario()
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )

        // When
        advanceTimeBy(BATCH_WAIT - 2)
        runCurrent()

        // Then
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(activity.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a change heard WHEN another comes within the batch window THEN both pull at once`() = runTest {
        // Given
        prepareScenario()
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(300L)
        runCurrent()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.PAYMENT_RECORDED,
                groupId = APE,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(setOf(FLORIPA, APE), groups.downloadedGroupIds.toSet())
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
    }

    @Test
    fun `GIVEN several events of one group in a burst WHEN they are heard THEN the group comes down once`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_CREATED,
            ),
        )
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
        assertEquals(listOf(FLORIPA), expenses.downloadedGroupIds)
    }

    @Test
    fun `GIVEN a change already pulled WHEN another is heard after the window THEN pulls again`() = runTest {
        // Given
        prepareScenario()
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
                groupId = APE,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA, APE), groups.downloadedGroupIds)
        assertEquals(2, activity.downloadedUserIds.size)
    }

    @Test
    fun `GIVEN listening WHEN a comment is heard THEN pulls the comments and the activity but no group`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.COMMENT_ADDED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billComments.downloadedUserIds)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(billAttachments.downloadedUserIds.isEmpty())
        assertEquals(0, account.registrationCount)
    }

    @Test
    fun `GIVEN listening WHEN a photo added to a bill is heard THEN pulls the attachments and the activity`() =
        runTest {
            // Given
            prepareScenario()

            // When
            remoteChanges.changes.emit(
                event(
                    kind = ActivityKind.ATTACHMENT_ADDED,
                ),
            )
            advanceTimeBy(BATCH_WAIT)
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), billAttachments.downloadedUserIds)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), activity.downloadedUserIds)
            assertTrue(groups.downloadedGroupIds.isEmpty())
            assertTrue(billComments.downloadedUserIds.isEmpty())
            assertEquals(0, account.registrationCount)
        }

    @Test
    fun `GIVEN listening WHEN a member who joined a group is heard THEN runs a full sync`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.MEMBER_JOINED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(SyncState.SYNCING in states)
    }

    @Test
    fun `GIVEN listening WHEN a group deleted is heard THEN runs a full sync`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.GROUP_DELETED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN listening WHEN an event of a kind a newer server records is heard THEN runs a full sync`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(
            event(
                kind = null,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN the group fails to come down WHEN its bill edited is heard THEN falls back to a full sync`() = runTest {
        // Given
        prepareScenario()
        groups.firstDownloadError = IllegalStateException("server down")

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN listening WHEN a member added is heard THEN pulls the group with the members photos and keys`() =
        runTest {
            // Given
            prepareScenario()

            // When
            remoteChanges.changes.emit(
                event(
                    kind = ActivityKind.MEMBER_ADDED,
                ),
            )
            advanceTimeBy(BATCH_WAIT)
            runCurrent()

            // Then
            assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
            assertEquals(1, groups.memberPhotoDownloads)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), paymentKeys.memberKeysDownloadedUserIds)
            assertEquals(0, account.registrationCount)
        }

    @Test
    fun `GIVEN listening WHEN the own profile saved on another device is heard THEN pulls the profile only`() =
        runTest {
            // Given
            prepareScenario()

            // When
            remoteChanges.changes.emit(RemoteChange.ProfileChanged(FakeAccountRepository.ANONYMOUS_USER_ID))
            advanceTimeBy(BATCH_WAIT)
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), profile.downloadedUserIds)
            assertEquals(0, groups.memberPhotoDownloads)
            assertTrue(activity.downloadedUserIds.isEmpty())
            assertEquals(0, account.registrationCount)
        }

    @Test
    fun `GIVEN listening WHEN someone else saved their profile THEN pulls the members photos only`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(RemoteChange.ProfileChanged("ana"))
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(1, groups.memberPhotoDownloads)
        assertTrue(profile.downloadedUserIds.isEmpty())
        assertEquals(0, account.registrationCount)
    }

    @Test
    fun `GIVEN a sync in flight WHEN a change is heard THEN the pull waits for the sync to end`() = runTest {
        // Given
        prepareScenario(syncInFlight = true)

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertTrue(groups.downloadedGroupIds.isEmpty())
        assertTrue(activity.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN a change heard during a sync WHEN the sync ends THEN its group comes down`() = runTest {
        // Given
        prepareScenario(syncInFlight = true)
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // When
        syncGate.complete(Unit)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
        assertEquals(1, account.registrationCount)
    }

    @Test
    fun `GIVEN listening WHEN the channel joins THEN catches up with a full sync`() = runTest {
        // Given
        prepareScenario()

        // When
        remoteChanges.changes.emit(RemoteChange.Joined)
        runCurrent()

        // Then
        assertEquals(1, account.registrationCount)
        assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN the channel joined before a sync in flight read the groups WHEN that sync ends THEN no catch up runs`() =
        runTest {
            // Given
            prepareScenario(syncInFlight = true)
            remoteChanges.changes.emit(RemoteChange.Joined)
            runCurrent()

            // When
            syncGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(1, account.registrationCount)
            assertEquals(listOf(FakeAccountRepository.ANONYMOUS_USER_ID), groups.downloadedUserIds)
        }

    @Test
    fun `GIVEN the channel joined while a sync read the groups WHEN that sync ends THEN still catches up after it`() =
        runTest {
            // Given
            prepareScenario(syncReadingGroups = true)
            remoteChanges.changes.emit(RemoteChange.Joined)
            runCurrent()

            // When
            syncGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(2, account.registrationCount)
            assertEquals(List(2) { FakeAccountRepository.ANONYMOUS_USER_ID }, groups.downloadedUserIds)
        }

    @Test
    fun `GIVEN a sync queued that begins after the channel joined WHEN the syncs run THEN no extra catch up runs`() =
        runTest {
            // Given
            prepareScenario(syncInFlight = true)
            coordinator.retry()
            runCurrent()
            remoteChanges.changes.emit(RemoteChange.Joined)
            runCurrent()

            // When
            syncGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(2, account.registrationCount)
            assertEquals(List(2) { FakeAccountRepository.ANONYMOUS_USER_ID }, groups.downloadedUserIds)
        }

    @Test
    fun `GIVEN the channel failed to listen WHEN the wait to listen again passes THEN listens again`() = runTest {
        // Given
        prepareScenario(failingListens = 1)

        // When
        advanceTimeBy(RELISTEN_MILLIS)
        runCurrent()

        // Then
        assertEquals(1, remoteChanges.listeners)
        assertEquals(2, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN the channel failed to listen WHEN less than the wait passes THEN does not listen yet`() = runTest {
        // Given
        prepareScenario(failingListens = 1)

        // When
        advanceTimeBy(RELISTEN_MILLIS - 1_000L)
        runCurrent()

        // Then
        assertEquals(0, remoteChanges.listeners)
        assertEquals(1, remoteChanges.joinCount)
    }

    @Test
    fun `GIVEN the channel listens again after failing WHEN a bill edited is heard THEN pulls its group`() = runTest {
        // Given
        prepareScenario(failingListens = 1)
        advanceTimeBy(RELISTEN_MILLIS)
        runCurrent()

        // When
        remoteChanges.changes.emit(
            event(
                kind = ActivityKind.BILL_EDITED,
            ),
        )
        advanceTimeBy(BATCH_WAIT)
        runCurrent()

        // Then
        assertEquals(listOf(FLORIPA), groups.downloadedGroupIds)
    }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)

    private fun event(
        kind: ActivityKind?,
        groupId: String = FLORIPA,
    ): RemoteChange.GroupEvent = RemoteChange.GroupEvent(
        groupId = groupId,
        kind = kind,
    )

    /*
     * Starts the coordinator and lets the launch's own sync settle, then forgets what it recorded, so each case sees
     * only what real time does. A sync in flight is held at its profile download until syncGate opens.
     */
    private fun TestScope.prepareScenario(
        status: AccountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
        storedProfile: UserProfile? = pierre,
        online: Boolean = true,
        inFront: Boolean = true,
        failingListens: Int = 0,
        syncInFlight: Boolean = false,
        syncReadingGroups: Boolean = false,
    ) {
        account = FakeAccountRepository(initialStatus = status)
        profile = FakeProfileRepository(initialProfile = storedProfile)
        gatedProfile = GatedDownloadProfileRepository(profile)
        groups = FakeGroupRepository()
        gatedGroups = GatedDownloadGroupRepository(groups)
        categories = FakeCategoryRepository()
        expenses = FakeExpenseRepository()
        settlements = FakeSettlementRepository()
        billComments = FakeBillCommentRepository()
        billAttachments = FakeBillAttachmentRepository()
        activity = FakeActivityRepository()
        paymentKeys = FakePaymentKeyRepository()
        remoteChanges = FakeRemoteChangeRepository().also { it.failingListens = failingListens }
        appForeground = AppForeground()
        isOnline = MutableStateFlow(online)
        coordinator = SyncCoordinator(
            accountRepository = account,
            profileRepository = gatedProfile,
            paymentKeyRepository = paymentKeys,
            appInviteRepository = FakeAppInviteRepository(),
            notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
            groupRepository = gatedGroups,
            groupPinRepository = FakeGroupPinRepository(),
            groupIncomeRepository = FakeGroupIncomeRepository(),
            expenseRepository = expenses,
            settlementRepository = settlements,
            categoryRepository = categories,
            personalCategoryData = PersonalCategoryData(FakeCategoryTermRepository(), FakeCustomColorRepository()),
            billCommentRepository = billComments,
            billAttachmentRepository = billAttachments,
            activityRepository = activity,
            hiddenContactRepository = FakeHiddenContactRepository(),
            pushDeviceRepository = FakePushDeviceRepository(),
            remoteChangeRepository = remoteChanges,
            firstDownload = FirstDownload(),
            appForeground = appForeground,
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            crashReporter = CrashReporter {},
            scope = backgroundScope,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator.observeState().collect(states::add)
        }
        coordinator.start()
        appForeground.update(inFront)
        runCurrent()
        account.registrationCount = 0
        profile.downloadedUserIds.clear()
        groups.downloadedUserIds.clear()
        groups.downloadedGroupIds.clear()
        groups.memberPhotoDownloads = 0
        categories.downloadedGroupIds.clear()
        expenses.downloadedUserIds.clear()
        expenses.downloadedGroupIds.clear()
        settlements.downloadedGroupIds.clear()
        billComments.downloadedUserIds.clear()
        billAttachments.downloadedUserIds.clear()
        activity.downloadedUserIds.clear()
        paymentKeys.memberKeysDownloadedUserIds.clear()
        states.clear()
        if (syncInFlight) {
            gatedProfile.gate = syncGate
            coordinator.retry()
            runCurrent()
        }
        if (syncReadingGroups) {
            gatedGroups.gate = syncGate
            coordinator.retry()
            runCurrent()
        }
    }

    private companion object {
        const val FLORIPA = "floripa"
        const val APE = "ape-302"

        // Just past the 500 ms a change heard waits for the rest of its burst.
        const val BATCH_WAIT = 501L

        // How long a channel that failed waits to listen again.
        const val RELISTEN_MILLIS = 30_000L
    }
}

// Holds the profile download, the first step of a sync after registering, while a gate is set and closed.
private class GatedDownloadGroupRepository(
    private val delegate: FakeGroupRepository,
) : GroupRepository by delegate {
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun downloadGroups(userId: String): Result<Unit> {
        gate?.await()
        return delegate.downloadGroups(userId)
    }
}

private class GatedDownloadProfileRepository(
    private val delegate: FakeProfileRepository,
) : ProfileRepository by delegate {
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun downloadProfile(userId: String): Result<Unit> {
        gate?.await()
        return delegate.downloadProfile(userId)
    }
}

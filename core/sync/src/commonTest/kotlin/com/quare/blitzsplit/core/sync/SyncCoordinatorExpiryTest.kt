package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.model.AccountExpiredException
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CustomColorRepository
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.InactivityExpiry
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * An account without Google or Apple that the server deleted after 30 days without a sync: what a sync of it tells
 * Crashlytics, and how SyncCoordinator erases the device's copy of an expired one.
 */
class SyncCoordinatorExpiryTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var paymentKeys: FakePaymentKeyRepository
    private lateinit var notificationPreferences: FakeNotificationPreferencesRepository
    private lateinit var appInvite: FakeAppInviteRepository
    private lateinit var categoryTerms: FakeCategoryTermRepository
    private lateinit var customColors: FakeCustomColorRepository
    private lateinit var payingMethods: FakePayingMethodRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var activity: FakeActivityRepository
    private lateinit var hiddenContacts: FakeHiddenContactRepository
    private lateinit var steps: EraseSteps
    private lateinit var syncGate: CompletableDeferred<Unit>
    private lateinit var coordinator: SyncCoordinator
    private val reportedFailures = mutableListOf<Throwable>()

    @Test
    fun `GIVEN an expired account WHEN syncing online THEN fails without sending it nor telling Crashlytics`() =
        runTest {
            // Given
            prepareScenario(expiry = InactivityExpiry.PENDING)
            account.registerError = AccountExpiredException()

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(emptyList(), reportedFailures)
            assertEquals(emptyList(), profile.uploadedUserIds)
        }

    @Test
    fun `GIVEN a sync failure caused by an expired account WHEN syncing online THEN does not tell Crashlytics`() =
        runTest {
            // Given
            prepareScenario(expiry = InactivityExpiry.PENDING)
            account.registerError = IllegalStateException("sync refused", AccountExpiredException())

            // When
            val result = coordinator.syncNow()

            // Then
            assertTrue(result.isFailure)
            assertEquals(emptyList(), reportedFailures)
        }

    @Test
    fun `GIVEN a sync that fails for another reason WHEN syncing online THEN tells Crashlytics`() = runTest {
        // Given
        prepareScenario()
        account.registerError = IllegalStateException("server down")

        // When
        val result = coordinator.syncNow()

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, reportedFailures.size)
        assertIs<SyncFailure>(reportedFailures.single())
    }

    @Test
    fun `GIVEN a sync of an expired account that failed WHEN the next one fails for another reason THEN tells it`() =
        runTest {
            // Given
            prepareScenario()
            account.registerError = AccountExpiredException()
            coordinator.syncNow()
            account.registerError = IllegalStateException("server down")

            // When
            coordinator.syncNow()

            // Then
            assertEquals(1, reportedFailures.size)
        }

    @Test
    fun `GIVEN an account that expires while the app runs WHEN it is marked THEN erases the whole device copy`() =
        runTest {
            // Given
            prepareScenario(storedKeys = listOf(pixKey))
            coordinator.start()
            runCurrent()

            // When
            account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertEquals(1, notificationPreferences.clearCount)
            assertEquals(1, appInvite.clearCount)
            assertTrue(categoryTerms.isCleared)
            assertTrue(customColors.isCleared)
            assertTrue(payingMethods.isCleared)
            assertEquals(1, groups.clearCount)
            assertEquals(1, activity.clearCalls)
            assertEquals(1, hiddenContacts.clearCount)
        }

    @Test
    fun `GIVEN an account that expires while the app runs WHEN it is marked THEN leaves the notice`() = runTest {
        // Given
        prepareScenario()
        coordinator.start()
        runCurrent()

        // When
        account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
        runCurrent()

        // Then
        assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN an account already expired WHEN the app starts THEN erases the device copy and leaves the notice`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                storedKeys = listOf(pixKey),
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN an account already expired WHEN the app starts THEN nothing of it goes up nor to Crashlytics`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                pendingChanges = true,
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(emptyList(), profile.uploadedUserIds)
            assertEquals(emptyList(), reportedFailures)
        }

    @Test
    fun `GIVEN an expired account WHEN it is erased THEN the keys go first and the profile last`() = runTest {
        // Given
        prepareScenario(expiry = InactivityExpiry.PENDING)

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals("keys", steps.attempts.first())
        assertEquals("profile", steps.attempts.last())
    }

    @Test
    fun `GIVEN an expired account WHEN it is erased THEN neither signs out nor deletes the account on the server`() =
        runTest {
            // Given
            prepareScenario(expiry = InactivityExpiry.PENDING)

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(0, account.signOutCount)
            assertEquals(0, account.deleteAccountCount)
        }

    @Test
    fun `GIVEN clearing the keys fails WHEN an expired account is erased THEN clears the rest and leaves the notice`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                failingSteps = setOf("keys"),
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertEquals(1, groups.clearCount)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN clearing the category map fails WHEN an expired account is erased THEN still clears the colors`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                failingSteps = setOf("category terms"),
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertTrue(customColors.isCleared)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN clearing the profile fails WHEN an expired account is erased THEN the rest goes and it stays pending`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                storedKeys = listOf(pixKey),
                failingSteps = setOf("profile"),
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertTrue(paymentKeys.keys.value.isEmpty())
            assertEquals(InactivityExpiry.PENDING, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN every clear fails WHEN an expired account is erased THEN tries each one and stays pending`() = runTest {
        // Given
        prepareScenario(
            expiry = InactivityExpiry.PENDING,
            failingSteps = deviceCopy.toSet(),
        )
        account.registerError = AccountExpiredException()

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(deviceCopy, steps.attempts)
        assertEquals(InactivityExpiry.PENDING, account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN the notice is left at once WHEN an expired account is erased THEN leaves it a single time`() = runTest {
        // Given
        prepareScenario(expiry = InactivityExpiry.PENDING)

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(1, steps.noticeAttempts)
        assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN leaving the notice fails once WHEN an expired account is erased THEN the second try leaves it`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                noticeFailures = 1,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(2, steps.noticeAttempts)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
            assertEquals(deviceCopy, steps.attempts)
        }

    @Test
    fun `GIVEN leaving the notice fails twice WHEN an expired account is erased THEN the third try leaves it`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                noticeFailures = 2,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(3, steps.noticeAttempts)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
            assertEquals(deviceCopy, steps.attempts)
        }

    @Test
    fun `GIVEN leaving the notice always fails WHEN an expired account is erased THEN gives up after three tries`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                noticeFailures = Int.MAX_VALUE,
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(3, steps.noticeAttempts)
            assertEquals(InactivityExpiry.PENDING, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN leaving the notice always fails WHEN an expired account is erased THEN the copy is erased only once`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                storedKeys = listOf(pixKey),
                noticeFailures = Int.MAX_VALUE,
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertTrue(paymentKeys.keys.value.isEmpty())
        }

    @Test
    fun `GIVEN clearing the profile fails WHEN an expired account is erased THEN never tries to leave the notice`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                failingSteps = setOf("profile"),
            )
            account.registerError = AccountExpiredException()

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(0, steps.noticeAttempts)
            assertEquals(pierre, profile.profile.value)
            assertEquals(emptyList(), profile.uploadedUserIds)
        }

    @Test
    fun `GIVEN a profile that failed to clear at the last launch WHEN the app starts again THEN erases it then`() =
        runTest {
            // Given
            prepareScenario(
                expiry = InactivityExpiry.PENDING,
                failingSteps = setOf("profile"),
            )
            account.registerError = AccountExpiredException()
            coordinator.start()
            runCurrent()
            steps.failing = emptySet()
            steps.attempts.clear()
            val relaunched = newCoordinator()

            // When
            relaunched.start()
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN an account with no expiry WHEN the app starts THEN erases nothing`() = runTest {
        // Given
        prepareScenario(storedKeys = listOf(pixKey))

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(emptyList(), steps.attempts)
        assertEquals(pierre, profile.profile.value)
        assertEquals(listOf(pixKey), paymentKeys.keys.value)
        assertNull(account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN a Google account with no expiry WHEN the app starts THEN erases nothing`() = runTest {
        // Given
        prepareScenario(status = google)

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(emptyList(), steps.attempts)
        assertEquals(pierre, profile.profile.value)
        assertEquals(google, account.status)
    }

    @Test
    fun `GIVEN the notice of an erased account still unread WHEN the app starts THEN erases nothing and keeps it`() =
        runTest {
            // Given
            prepareScenario(
                storedProfile = null,
                status = AccountStatus.LocalOnly,
                expiry = InactivityExpiry.NOTICE,
            )

            // When
            coordinator.start()
            runCurrent()

            // Then
            assertEquals(emptyList(), steps.attempts)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN a new profile made behind the notice WHEN the app starts THEN keeps the profile`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.LocalOnly,
            expiry = InactivityExpiry.NOTICE,
        )

        // When
        coordinator.start()
        runCurrent()

        // Then
        assertEquals(emptyList(), steps.attempts)
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN the coordinator was never started WHEN the account expires THEN erases nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
        runCurrent()

        // Then
        assertEquals(emptyList(), steps.attempts)
        assertEquals(pierre, profile.profile.value)
        assertEquals(InactivityExpiry.PENDING, account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN an erased account whose notice was read WHEN a later account expires THEN erases the device again`() =
        runTest {
            // Given
            prepareScenario(expiry = InactivityExpiry.PENDING)
            coordinator.start()
            runCurrent()
            account.dismissExpiredNotice()
            profile.profile.value = pierre
            runCurrent()
            steps.attempts.clear()

            // When
            account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN a sync in flight WHEN the account expires THEN erases nothing until it ends`() = runTest {
        // Given
        prepareScenario(syncHangs = true)
        coordinator.start()
        runCurrent()

        // When
        account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
        runCurrent()

        // Then
        assertEquals(emptyList(), steps.attempts)
        assertEquals(pierre, profile.profile.value)
        assertEquals(InactivityExpiry.PENDING, account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN an expired account waiting on a sync WHEN the sync ends THEN erases the copy and leaves the notice`() =
        runTest {
            // Given
            prepareScenario(syncHangs = true)
            coordinator.start()
            runCurrent()
            account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
            runCurrent()

            // When
            syncGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(deviceCopy, steps.attempts)
            assertNull(profile.profile.value)
            assertEquals(InactivityExpiry.NOTICE, account.snapshot.value.inactivityExpiry)
        }

    @Test
    fun `GIVEN a sign out that got ahead of the expiry WHEN both get their turn THEN erases once and no notice`() =
        runTest {
            // Given
            prepareScenario(syncHangs = true)
            coordinator.start()
            runCurrent()
            backgroundScope.launch { coordinator.signOut() }
            runCurrent()
            account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.PENDING) }
            runCurrent()

            // When
            syncGate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf("account sign out") + deviceCopy, steps.attempts)
            assertEquals(1, profile.clearCount)
            assertEquals(0, steps.noticeAttempts)
            assertNull(account.snapshot.value.inactivityExpiry)
            assertEquals(AccountStatus.LocalOnly, account.status)
        }

    @Test
    fun `GIVEN an account WHEN signing out THEN ends the session and then erases the whole device copy`() = runTest {
        // Given
        prepareScenario(
            status = google,
            storedKeys = listOf(pixKey),
        )

        // When
        val result = coordinator.signOut()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("account sign out") + deviceCopy, steps.attempts)
        assertNull(profile.profile.value)
        assertTrue(paymentKeys.keys.value.isEmpty())
        assertNull(account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN clearing the preferences fails WHEN signing out THEN fails and stops there`() = runTest {
        // Given
        prepareScenario(
            status = google,
            failingSteps = setOf("preferences"),
        )

        // When
        val result = coordinator.signOut()

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf("account sign out", "keys", "preferences"), steps.attempts)
        assertEquals(pierre, profile.profile.value)
    }

    @Test
    fun `GIVEN an account WHEN deleting it THEN the server goes first and then the whole device copy`() = runTest {
        // Given
        prepareScenario(
            status = google,
            storedKeys = listOf(pixKey),
        )

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("account delete") + deviceCopy + "account end deleted session", steps.attempts)
        assertNull(profile.profile.value)
        assertTrue(paymentKeys.keys.value.isEmpty())
        assertNull(account.snapshot.value.inactivityExpiry)
    }

    @Test
    fun `GIVEN every clear fails WHEN deleting the account THEN tries each one and still ends the session`() = runTest {
        // Given
        prepareScenario(
            status = google,
            failingSteps = deviceCopy.toSet(),
        )

        // When
        val result = coordinator.deleteAccount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("account delete") + deviceCopy + "account end deleted session", steps.attempts)
        assertEquals(AccountStatus.LocalOnly, account.status)
    }

    private val anonymous = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID)
    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)
    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val pixKey = PaymentKey(
        id = "pix-email",
        type = PaymentKeyType.EMAIL,
        value = "pierre@example.com",
        isDefault = true,
        addedAt = 1L,
    )

    // What "Delete account" erases on the device, in its order: the keys first, the profile last.
    private val deviceCopy = listOf(
        "keys",
        "preferences",
        "app invite",
        "category terms",
        "custom colors",
        "paying methods",
        "groups",
        "activity",
        "hidden contacts",
        "profile",
    )

    // Why: another launch of the app over the same device: what it stored stays, the coordinator is new.
    private fun TestScope.newCoordinator(
        online: Boolean = true,
        syncHangs: Boolean = false,
    ): SyncCoordinator = SyncCoordinator(
        accountRepository = StepAccountRepository(account, steps),
        profileRepository = StepProfileRepository(
            delegate = profile,
            steps = steps,
            downloadGate = syncGate.takeIf { syncHangs },
        ),
        paymentKeyRepository = StepPaymentKeyRepository(paymentKeys, steps),
        notificationPreferencesRepository = StepNotificationPreferencesRepository(notificationPreferences, steps),
        appInviteRepository = StepAppInviteRepository(appInvite, steps),
        groupRepository = StepGroupRepository(groups, steps),
        groupPinRepository = FakeGroupPinRepository(),
        groupIncomeRepository = FakeGroupIncomeRepository(),
        expenseRepository = FakeExpenseRepository(),
        settlementRepository = FakeSettlementRepository(),
        categoryRepository = FakeCategoryRepository(),
        personalCategoryData = PersonalCategoryData(
            StepCategoryTermRepository(categoryTerms, steps),
            StepCustomColorRepository(customColors, steps),
        ),
        payingMethodRepository = StepPayingMethodRepository(payingMethods, steps),
        billCommentRepository = FakeBillCommentRepository(),
        billAttachmentRepository = FakeBillAttachmentRepository(),
        activityRepository = StepActivityRepository(activity, steps),
        hiddenContactRepository = StepHiddenContactRepository(hiddenContacts, steps),
        pushDeviceRepository = FakePushDeviceRepository(),
        remoteChangeRepository = FakeRemoteChangeRepository(),
        firstDownload = FirstDownload(),
        appForeground = AppForeground(),
        connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        crashReporter = CrashReporter { reportedFailures += it },
        scope = backgroundScope,
    )

    private fun TestScope.prepareScenario(
        storedProfile: UserProfile? = pierre,
        status: AccountStatus = anonymous,
        expiry: InactivityExpiry? = null,
        online: Boolean = true,
        pendingChanges: Boolean = false,
        storedKeys: List<PaymentKey> = emptyList(),
        failingSteps: Set<String> = emptySet(),
        noticeFailures: Int = 0,
        syncHangs: Boolean = false,
    ) {
        steps = EraseSteps(failingSteps, noticeFailures)
        syncGate = CompletableDeferred()
        account = FakeAccountRepository(initialStatus = status).also { fake ->
            fake.snapshot.update { it.copy(inactivityExpiry = expiry) }
        }
        profile = FakeProfileRepository(initialProfile = storedProfile).also {
            it.hasPendingChanges.value = pendingChanges
        }
        paymentKeys = FakePaymentKeyRepository(initialKeys = storedKeys)
        notificationPreferences = FakeNotificationPreferencesRepository()
        appInvite = FakeAppInviteRepository()
        categoryTerms = FakeCategoryTermRepository()
        customColors = FakeCustomColorRepository()
        payingMethods = FakePayingMethodRepository()
        groups = FakeGroupRepository()
        activity = FakeActivityRepository()
        hiddenContacts = FakeHiddenContactRepository()
        coordinator = newCoordinator(online, syncHangs)
    }
}

/*
 * Why: one log of what ends the session and of each part of the device's copy a clear was asked of, in order, with
 * the ones a case makes fail, so a test sees which ones were still tried after a failure.
 */
private class EraseSteps(
    var failing: Set<String>,
    private val noticeFailures: Int,
) {
    val attempts = mutableListOf<String>()
    var noticeAttempts = 0
        private set

    // Why: apart from the log, so the log of an expiry holds only the device's copy; the first tries fail when a
    // case asks.
    fun attemptNotice() {
        noticeAttempts++
        if (noticeAttempts <= noticeFailures) throw IllegalStateException("database closed")
    }

    fun attempt(step: String) {
        attempts += step
        if (step in failing) throw IllegalStateException("disk full")
    }
}

private class StepAccountRepository(
    private val delegate: AccountRepository,
    private val steps: EraseSteps,
) : AccountRepository by delegate {
    override suspend fun signOut() {
        steps.attempt("account sign out")
        delegate.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        steps.attempt("account delete")
        return delegate.deleteAccount()
    }

    override suspend fun endDeletedSession() {
        steps.attempt("account end deleted session")
        delegate.endDeletedSession()
    }

    override suspend fun endExpiredSession() {
        steps.attemptNotice()
        delegate.endExpiredSession()
    }
}

// Why: also holds a sync mid-flight until the gate opens, so a test can expire the account while it runs.
private class StepProfileRepository(
    private val delegate: ProfileRepository,
    private val steps: EraseSteps,
    private val downloadGate: CompletableDeferred<Unit>?,
) : ProfileRepository by delegate {
    override suspend fun downloadProfile(userId: String): Result<Unit> {
        downloadGate?.await()
        return delegate.downloadProfile(userId)
    }

    override suspend fun clear() {
        steps.attempt("profile")
        delegate.clear()
    }
}

private class StepPaymentKeyRepository(
    private val delegate: PaymentKeyRepository,
    private val steps: EraseSteps,
) : PaymentKeyRepository by delegate {
    override suspend fun clear() {
        steps.attempt("keys")
        delegate.clear()
    }
}

private class StepNotificationPreferencesRepository(
    private val delegate: NotificationPreferencesRepository,
    private val steps: EraseSteps,
) : NotificationPreferencesRepository by delegate {
    override suspend fun clear() {
        steps.attempt("preferences")
        delegate.clear()
    }
}

private class StepAppInviteRepository(
    private val delegate: AppInviteRepository,
    private val steps: EraseSteps,
) : AppInviteRepository by delegate {
    override suspend fun clear() {
        steps.attempt("app invite")
        delegate.clear()
    }
}

private class StepCategoryTermRepository(
    private val delegate: CategoryTermRepository,
    private val steps: EraseSteps,
) : CategoryTermRepository by delegate {
    override suspend fun clear() {
        steps.attempt("category terms")
        delegate.clear()
    }
}

private class StepCustomColorRepository(
    private val delegate: CustomColorRepository,
    private val steps: EraseSteps,
) : CustomColorRepository by delegate {
    override suspend fun clear() {
        steps.attempt("custom colors")
        delegate.clear()
    }
}

private class StepPayingMethodRepository(
    private val delegate: PayingMethodRepository,
    private val steps: EraseSteps,
) : PayingMethodRepository by delegate {
    override suspend fun clear() {
        steps.attempt("paying methods")
        delegate.clear()
    }
}

private class StepGroupRepository(
    private val delegate: GroupRepository,
    private val steps: EraseSteps,
) : GroupRepository by delegate {
    override suspend fun clear() {
        steps.attempt("groups")
        delegate.clear()
    }
}

private class StepActivityRepository(
    private val delegate: ActivityRepository,
    private val steps: EraseSteps,
) : ActivityRepository by delegate {
    override suspend fun clear() {
        steps.attempt("activity")
        delegate.clear()
    }
}

private class StepHiddenContactRepository(
    private val delegate: HiddenContactRepository,
    private val steps: EraseSteps,
) : HiddenContactRepository by delegate {
    override suspend fun clear() {
        steps.attempt("hidden contacts")
        delegate.clear()
    }
}

package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactGroup
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.repository.GuestMergeRepository
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ClaimGuestAsMeUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClaimGuestAsMeUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var merges: FakeGuestMergeRepository
    private lateinit var gate: GatedGuestMergeRepository
    private lateinit var claimGuestAsMe: ClaimGuestAsMeUseCase

    private val diego = contact(
        key = "name:diego",
        name = "Diego",
        groups = listOf(contactGroup("ape", "diego-ape"), contactGroup("futebol", "diego-futebol")),
    )

    @Test
    fun `GIVEN someone with an account WHEN claiming them THEN fails without asking the server`() = runTest {
        // Given
        prepareScenario()
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            groups = listOf(contactGroup("ape", "carla-ape")),
        )

        // When
        val result = claimGuestAsMe(carla)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(merges.claimedMemberIds.isEmpty())
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN changes not pushed yet WHEN claiming a guest THEN pushes them before asking the server`() = runTest {
        // Given
        prepareScenario(gated = true)
        groups.hasPendingChanges.value = true
        gate.answer.complete(Unit)

        // When
        val result = claimGuestAsMe(diego)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(1), gate.uploadsSeenWhenAsked)
    }

    @Test
    fun `GIVEN a guest with the same member listed twice WHEN claiming them THEN asks for each member once`() =
        runTest {
            // Given
            prepareScenario()
            val repeated = contact(
                key = "name:diego",
                name = "Diego",
                groups = listOf(contactGroup("ape", "diego-ape"), contactGroup("futebol", "diego-ape")),
            )

            // When
            val result = claimGuestAsMe(repeated)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(listOf("diego-ape")), merges.claimedMemberIds)
        }

    @Test
    fun `GIVEN the server moves the members WHEN claiming a guest THEN returns how many moved and syncs once`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = claimGuestAsMe(diego)

            // Then
            assertEquals(Result.success(2), result)
            assertEquals(listOf(listOf("diego-ape", "diego-futebol")), merges.claimedMemberIds)
            assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
        }

    @Test
    fun `GIVEN the server moves no member WHEN claiming a guest THEN succeeds with zero and still syncs`() = runTest {
        // Given
        prepareScenario(claimedCount = 0)

        // When
        val result = claimGuestAsMe(diego)

        // Then
        assertEquals(Result.success(0), result)
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
    }

    @Test
    fun `GIVEN the server fails WHEN claiming a guest THEN returns that failure and does not sync again`() = runTest {
        // Given
        prepareScenario(claimError = IllegalStateException("offline"))

        // When
        val result = claimGuestAsMe(diego)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(groups.downloadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the push fails WHEN claiming a guest THEN fails without asking the server`() = runTest {
        // Given
        prepareScenario(pushError = IllegalStateException("offline"))
        groups.hasPendingChanges.value = true

        // When
        val result = claimGuestAsMe(diego)

        // Then
        assertTrue(result.isFailure)
        assertTrue(merges.claimedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN the caller leaves mid request WHEN the server answers THEN the claim still completes and syncs`() =
        runTest {
            // Given
            prepareScenario(gated = true)
            var result: Result<Int>? = null
            val caller = launch { result = claimGuestAsMe(diego) }
            runCurrent()
            caller.cancel()
            runCurrent()

            // When
            gate.answer.complete(Unit)
            runCurrent()

            // Then
            assertEquals(listOf(listOf("diego-ape", "diego-futebol")), gate.asked)
            assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
            assertEquals(Result.success(2), result)
        }

    private fun contactGroup(
        groupId: String,
        memberId: String,
    ): ContactGroup = ContactGroup(
        groupId = groupId,
        groupName = groupId,
        groupInitials = "",
        groupColor = AvatarColor.GREEN,
        groupPhotoUrl = null,
        currency = GroupCurrency.BRL,
        memberId = memberId,
        meMemberId = "me-$groupId",
        groupInviteCode = null,
        memberInviteCode = null,
        meInviteCode = null,
        amountMinor = 0L,
    )

    private fun contact(
        key: String,
        name: String,
        userId: String? = null,
        groups: List<ContactGroup> = emptyList(),
    ): Contact = Contact(
        key = key,
        name = name,
        color = AvatarColor.BLUE,
        photoUrl = null,
        userId = userId,
        groups = groups,
        nets = listOf(ContactNet(GroupCurrency.BRL, 0L)),
        isHidden = false,
    )

    private fun TestScope.prepareScenario(
        pushError: Throwable? = null,
        claimError: Throwable? = null,
        claimedCount: Int? = null,
        gated: Boolean = false,
    ) {
        val account = FakeAccountRepository(
            initialStatus = AccountStatus.Anonymous(FakeAccountRepository.RESTORED_USER_ID),
        )
        val profile = FakeProfileRepository(
            initialProfile = UserProfile(
                displayName = "Pierre",
                avatarColor = AvatarColor.LILAC,
                photo = null,
            ),
        )
        groups = FakeGroupRepository().also { it.pushError = pushError }
        merges = FakeGuestMergeRepository().also {
            it.claimError = claimError
            it.claimedCount = claimedCount
        }
        gate = GatedGuestMergeRepository(groups)
        val coordinator = SyncCoordinator(
            accountRepository = account,
            profileRepository = profile,
            paymentKeyRepository = FakePaymentKeyRepository(),
            appInviteRepository = FakeAppInviteRepository(),
            notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
            groupRepository = groups,
            groupPinRepository = FakeGroupPinRepository(),
            groupIncomeRepository = FakeGroupIncomeRepository(),
            expenseRepository = FakeExpenseRepository(),
            settlementRepository = FakeSettlementRepository(),
            categoryRepository = FakeCategoryRepository(),
            categoryTermRepository = FakeCategoryTermRepository(),
            billCommentRepository = FakeBillCommentRepository(),
            billAttachmentRepository = FakeBillAttachmentRepository(),
            activityRepository = FakeActivityRepository(),
            hiddenContactRepository = FakeHiddenContactRepository(),
            pushDeviceRepository = FakePushDeviceRepository(),
            remoteChangeRepository = FakeRemoteChangeRepository(),
            firstDownload = FirstDownload(),
            appForeground = AppForeground(),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            crashReporter = CrashReporter {},
            scope = backgroundScope,
        )
        claimGuestAsMe = ClaimGuestAsMeUseCase(
            repository = if (gated) gate else merges,
            coordinator = coordinator,
        )
    }
}

// Why: answers a claim only once the test lets it, and notes how many uploads had happened when it was asked.
private class GatedGuestMergeRepository(
    private val groups: FakeGroupRepository,
) : GuestMergeRepository {
    val answer = CompletableDeferred<Unit>()
    val asked = mutableListOf<List<String>>()
    val uploadsSeenWhenAsked = mutableListOf<Int>()

    override suspend fun requestClaim(memberIds: List<String>): Result<Int> {
        asked += memberIds
        uploadsSeenWhenAsked += groups.uploadedUserIds.size
        answer.await()
        return Result.success(memberIds.size)
    }

    override suspend fun request(memberIds: List<String>): Result<GuestMerge> = error("unused")

    override suspend fun findPending(memberIds: List<String>): Result<GuestMerge?> = error("unused")

    override suspend fun cancel(mergeId: String): Result<Unit> = error("unused")
}

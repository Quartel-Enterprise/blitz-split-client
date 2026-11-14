package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillAttachmentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeBillCommentRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupDeleteFailure
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.sync.domain.usecase.impl.DeleteGroupUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.LeaveGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupExitUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var leaveGroup: LeaveGroupUseCase
    private lateinit var deleteGroup: DeleteGroupUseCase
    private lateinit var deletions: InMemoryGroupDeletionRepository

    @Test
    fun `GIVEN changes not pushed yet WHEN leaving THEN pushes them before asking the server`() = runTest {
        // Given
        prepareScenario()
        groups.hasPendingChanges.value = true

        // When
        val result = leaveGroup("group-1")

        // Then
        assertEquals(Result.success(GroupExitOutcome.DONE), result)
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.uploadedUserIds)
        assertEquals(listOf("group-1"), groups.leftGroupIds)
    }

    @Test
    fun `GIVEN the sync fails WHEN leaving THEN fails without asking the server`() = runTest {
        // Given
        prepareScenario(pushError = IllegalStateException("offline"))
        groups.hasPendingChanges.value = true

        // When
        val result = leaveGroup("group-1")

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.leftGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN nothing waits to go up WHEN deleting THEN hides the group at once and asks with no sync`() = runTest {
        // Given
        prepareScenario()

        // When
        deleteGroup("group-1", "Trip")
        val hiddenBeforeAnswer = groups.hiddenGroupIds.value
        runCurrent()

        // Then
        assertEquals(setOf("group-1"), hiddenBeforeAnswer)
        assertTrue(groups.downloadedUserIds.isEmpty())
        assertTrue(groups.uploadedUserIds.isEmpty())
        assertEquals(listOf("group-1"), groups.deletedGroupIds)
        assertTrue(deletions.observeDeletions().first().isEmpty())
        assertEquals(setOf("group-1"), groups.hiddenGroupIds.value)
    }

    @Test
    fun `GIVEN someone still owes on the server WHEN deleting THEN syncs the bill in and shows the group again`() =
        runTest {
            // Given
            prepareScenario(exitOutcome = GroupExitOutcome.HAS_BALANCE)

            // When
            deleteGroup("group-1", "Trip")
            runCurrent()

            // Then
            assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
            assertEquals(listOf("group-1"), groups.deletedGroupIds)
            assertTrue(groups.hiddenGroupIds.value.isEmpty())
            assertEquals(
                listOf(
                    GroupDeletion(
                        groupId = "group-1",
                        groupName = "Trip",
                        deletedByName = null,
                        failure = GroupDeleteFailure.HAS_BALANCE,
                    ),
                ),
                deletions.observeDeletions().first(),
            )
        }

    @Test
    fun `GIVEN changes waiting and the sync fails WHEN deleting THEN asks nothing and shows the group again`() =
        runTest {
            // Given
            prepareScenario(pushError = IllegalStateException("offline"))
            groups.hasPendingChanges.value = true

            // When
            deleteGroup("group-1", "Trip")
            runCurrent()

            // Then
            assertTrue(groups.deletedGroupIds.isEmpty())
            assertEquals(1, groups.downloadedUserIds.size)
            assertTrue(groups.hiddenGroupIds.value.isEmpty())
            assertEquals(
                listOf(GroupDeleteFailure.FAILED),
                deletions.observeDeletions().first().map { it.failure },
            )
        }

    @Test
    fun `GIVEN the answer lost on the way WHEN the sync finds the group gone THEN says nothing`() = runTest {
        // Given
        prepareScenario()
        groups.exitError = IllegalStateException("timeout")
        groups.serverDeletedGroupIds += "group-1"

        // When
        deleteGroup("group-1", "Trip")
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.RESTORED_USER_ID), groups.downloadedUserIds)
        assertTrue(deletions.observeDeletions().first().isEmpty())
    }

    @Test
    fun `GIVEN a notice still queued from an earlier try WHEN deleting again THEN that notice goes`() = runTest {
        // Given
        prepareScenario()
        deletions.post(
            GroupDeletion(
                groupId = "group-1",
                groupName = "Trip",
                deletedByName = null,
                failure = GroupDeleteFailure.FAILED,
            ),
        )

        // When
        deleteGroup("group-1", "Trip")

        // Then
        assertTrue(deletions.observeDeletions().first().isEmpty())
    }

    private val trip = Group(
        id = "group-1",
        name = "Trip",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = emptyList(),
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

    private fun TestScope.prepareScenario(
        pushError: Throwable? = null,
        exitOutcome: GroupExitOutcome = GroupExitOutcome.DONE,
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
        groups = FakeGroupRepository(initialGroups = listOf(trip)).also {
            it.pushError = pushError
            it.exitOutcome = exitOutcome
        }
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
            personalCategoryData = PersonalCategoryData(FakeCategoryTermRepository(), FakeCustomColorRepository()),
            payingMethodRepository = FakePayingMethodRepository(),
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
        leaveGroup = LeaveGroupUseCase(
            groupRepository = groups,
            coordinator = coordinator,
        )
        deletions = InMemoryGroupDeletionRepository()
        deleteGroup = DeleteGroupUseCase(
            groupRepository = groups,
            deletionRepository = deletions,
            coordinator = coordinator,
        )
    }
}

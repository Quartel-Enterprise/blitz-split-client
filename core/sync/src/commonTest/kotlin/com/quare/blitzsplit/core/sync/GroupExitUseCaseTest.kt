package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.impl.DeleteGroupUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.LeaveGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupExitUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var leaveGroup: LeaveGroupUseCase
    private lateinit var deleteGroup: DeleteGroupUseCase

    @Test
    fun `GIVEN changes not pushed yet WHEN leaving THEN pushes them before asking the server`() = runTest {
        // Given
        prepareScenario()

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

        // When
        val result = leaveGroup("group-1")

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.leftGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN someone still owes on the server WHEN deleting THEN answers it and syncs again to bring the bill`() =
        runTest {
            // Given
            prepareScenario(exitOutcome = GroupExitOutcome.HAS_BALANCE)

            // When
            val result = deleteGroup("group-1")

            // Then
            assertEquals(Result.success(GroupExitOutcome.HAS_BALANCE), result)
            assertEquals(List(2) { FakeAccountRepository.RESTORED_USER_ID }, groups.downloadedUserIds)
            assertEquals(listOf("group-1"), groups.deletedGroupIds)
        }

    @Test
    fun `GIVEN the sync fails WHEN deleting THEN fails without asking the server`() = runTest {
        // Given
        prepareScenario(pushError = IllegalStateException("offline"))

        // When
        val result = deleteGroup("group-1")

        // Then
        assertTrue(result.isFailure)
        assertTrue(groups.deletedGroupIds.isEmpty())
    }

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
        groups = FakeGroupRepository().also {
            it.pushError = pushError
            it.exitOutcome = exitOutcome
        }
        val coordinator = SyncCoordinator(
            accountRepository = account,
            profileRepository = profile,
            paymentKeyRepository = FakePaymentKeyRepository(),
            notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
            groupRepository = groups,
            expenseRepository = FakeExpenseRepository(),
            settlementRepository = FakeSettlementRepository(),
            categoryRepository = FakeCategoryRepository(),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
            scope = backgroundScope,
        )
        leaveGroup = LeaveGroupUseCase(
            groupRepository = groups,
            coordinator = coordinator,
        )
        deleteGroup = DeleteGroupUseCase(
            groupRepository = groups,
            coordinator = coordinator,
        )
    }
}

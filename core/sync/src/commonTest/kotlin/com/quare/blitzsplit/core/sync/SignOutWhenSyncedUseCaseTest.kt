package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SignOutWhenSyncedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignOutWhenSyncedUseCaseTest {
    private val paymentKeys = FakePaymentKeyRepository()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var useCase: SignOutWhenSyncedUseCase

    @Test
    fun `GIVEN a queued change online WHEN signing out once synced THEN pushes it and signs out`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase()
        runCurrent()

        // Then
        assertEquals(listOf(FakeAccountRepository.CONNECTED_USER_ID), profile.uploadedUserIds)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN the push fails WHEN signing out once synced THEN keeps the profile and the schedule`() = runTest {
        // Given
        prepareScenario()
        profile.pushError = IllegalStateException("server down")

        // When
        useCase()
        runCurrent()

        // Then
        assertTrue(account.signOutWhenSynced)
        assertEquals(pierre, profile.profile.value)
    }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)

    private fun TestScope.prepareScenario() {
        account = FakeAccountRepository(
            initialStatus = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        )
        profile = FakeProfileRepository(initialProfile = pierre).also { it.hasPendingChanges.value = true }
        useCase = SignOutWhenSyncedUseCase(
            SyncCoordinator(
                accountRepository = account,
                profileRepository = profile,
                paymentKeyRepository = paymentKeys,
                notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
                groupRepository = FakeGroupRepository(),
                expenseRepository = FakeExpenseRepository(),
                settlementRepository = FakeSettlementRepository(),
                activityRepository = FakeActivityRepository(),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                scope = backgroundScope,
            ),
        )
    }
}

package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
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
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignOutUseCaseTest {
    private val paymentKeys = FakePaymentKeyRepository()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var useCase: SignOutUseCase

    @Test
    fun `GIVEN a Google account WHEN signing out THEN goes back to local only and erases the profile`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase()

        // Then
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)

    private fun TestScope.prepareScenario() {
        account = FakeAccountRepository(
            initialStatus = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        )
        profile = FakeProfileRepository(initialProfile = pierre)
        useCase = SignOutUseCase(
            SyncCoordinator(
                accountRepository = account,
                profileRepository = profile,
                paymentKeyRepository = paymentKeys,
                notificationPreferencesRepository = FakeNotificationPreferencesRepository(),
                groupRepository = FakeGroupRepository(),
                expenseRepository = FakeExpenseRepository(),
                settlementRepository = FakeSettlementRepository(),
                categoryRepository = FakeCategoryRepository(),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                scope = backgroundScope,
            ),
        )
    }
}

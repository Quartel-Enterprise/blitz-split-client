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
import com.quare.blitzsplit.core.sync.domain.usecase.impl.DeleteAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountUseCaseTest {
    private val paymentKeys = FakePaymentKeyRepository()

    private lateinit var account: FakeAccountRepository
    private lateinit var profile: FakeProfileRepository
    private lateinit var useCase: DeleteAccountUseCase

    @Test
    fun `GIVEN a Google account WHEN deleting the account THEN deletes it and goes back to no account`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, account.deleteAccountCount)
        assertEquals(AccountStatus.LocalOnly, account.status)
        assertNull(profile.profile.value)
    }

    @Test
    fun `GIVEN the server fails WHEN deleting the account THEN returns the failure and keeps the profile`() = runTest {
        // Given
        prepareScenario(deleteError = IllegalStateException("server down"))

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(google, account.status)
        assertEquals(pierre, profile.profile.value)
    }

    private val pierre = UserProfile("Pierre", AvatarColor.BLUE, photo = null)
    private val google = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

    private fun TestScope.prepareScenario(deleteError: Throwable? = null) {
        account = FakeAccountRepository(initialStatus = google).also { it.deleteAccountError = deleteError }
        profile = FakeProfileRepository(initialProfile = pierre)
        useCase = DeleteAccountUseCase(
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

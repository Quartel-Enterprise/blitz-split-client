package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ObserveAccountSignOutsUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveAccountSignOutsUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private val signOuts = mutableListOf<String>()

    @Test
    fun `GIVEN an anonymous account WHEN it signs out THEN emits its user id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertEquals(listOf(PIERRE_ID), signOuts)
    }

    @Test
    fun `GIVEN a Google account WHEN it signs out THEN emits its user id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertEquals(listOf(PIERRE_ID), signOuts)
    }

    @Test
    fun `GIVEN an Apple account WHEN it is deleted THEN emits its user id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.APPLE))

        // When
        account.endDeletedSession()

        // Then
        assertEquals(listOf(PIERRE_ID), signOuts)
    }

    @Test
    fun `GIVEN a local only install WHEN observing sign outs THEN emits nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertTrue(signOuts.isEmpty())
    }

    @Test
    fun `GIVEN a Google account WHEN another user signs in instead THEN emits nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

        // When
        account.status = AccountStatus.Connected(ANA_ID, AccountProvider.APPLE)

        // Then
        assertTrue(signOuts.isEmpty())
    }

    @Test
    fun `GIVEN a user switched for another WHEN signing out THEN emits only the last user id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))
            account.status = AccountStatus.Anonymous(ANA_ID)

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            assertEquals(listOf(ANA_ID), signOuts)
        }

    @Test
    fun `GIVEN a signed out account WHEN the local only status comes again THEN emits the sign out once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))
            account.status = AccountStatus.LocalOnly

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            assertEquals(listOf(PIERRE_ID), signOuts)
        }

    @Test
    fun `GIVEN a user who signed out and in again WHEN signing out again THEN emits each sign out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))
            account.status = AccountStatus.LocalOnly
            account.status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE)

            // When
            account.status = AccountStatus.LocalOnly

            // Then
            assertEquals(listOf(PIERRE_ID, PIERRE_ID), signOuts)
        }

    private fun TestScope.prepareScenario(status: AccountStatus) {
        account = FakeAccountRepository(initialStatus = status)
        val useCase = ObserveAccountSignOutsUseCase(accountRepository = account)
        backgroundScope.launch { useCase().collect(signOuts::add) }
    }

    private companion object {
        const val PIERRE_ID = "pierre"
        const val ANA_ID = "ana"
    }
}

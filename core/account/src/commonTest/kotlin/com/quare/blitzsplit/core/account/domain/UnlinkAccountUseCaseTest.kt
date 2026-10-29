package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.UnlinkAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UnlinkAccountUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var useCase: UnlinkAccountUseCase

    @Test
    fun `GIVEN a Google account WHEN disconnecting it THEN removes the identity and makes the new code due`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase()

            // Then
            val snapshot = account.observeAccount().first()
            assertTrue(result.isSuccess)
            assertEquals(1, account.unlinkCount)
            assertEquals(AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID), snapshot.status)
            assertEquals(RecoveryCodePrompt.DUE, snapshot.recoveryCodePrompt)
        }

    @Test
    fun `GIVEN no internet WHEN disconnecting the account THEN fails`() = runTest {
        // Given
        prepareScenario(unlinkError = IllegalStateException("offline"))

        // When
        val result = useCase()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(1, account.unlinkCount)
    }

    private fun prepareScenario(unlinkError: Throwable? = null) {
        account = FakeAccountRepository(
            initialStatus = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        ).also { it.unlinkError = unlinkError }
        useCase = UnlinkAccountUseCase(account)
    }
}

package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.watch.domain.usecase.impl.SignInWithWatchPairingUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SignInWithWatchPairingUseCaseTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var useCase: SignInWithWatchPairingUseCase
    private var completeSignInCount = 0

    @Test
    fun `GIVEN an approved code WHEN signing in on the watch THEN uses its token hash and pulls the profile`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = useCase(TOKEN_HASH)

            // Then
            assertEquals(Result.success(Unit), result)
            assertEquals(listOf(TOKEN_HASH), account.pairingTokens)
            assertEquals(1, completeSignInCount)
        }

    @Test
    fun `GIVEN the token hash is refused WHEN signing in on the watch THEN fails without pulling the profile`() =
        runTest {
            // Given
            prepareScenario(pairingSignInError = IllegalStateException("token used"))

            // When
            val result = useCase(TOKEN_HASH)

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(0, completeSignInCount)
        }

    @Test
    fun `GIVEN the profile can not be pulled WHEN signing in on the watch THEN fails`() = runTest {
        // Given
        prepareScenario(completeSignInResult = Result.failure(IllegalArgumentException("pull failed")))

        // When
        val result = useCase(TOKEN_HASH)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals(listOf(TOKEN_HASH), account.pairingTokens)
    }

    private fun prepareScenario(
        pairingSignInError: Throwable? = null,
        completeSignInResult: Result<SignInOutcome> = Result.success(SignInOutcome.PROFILE_RESTORED),
    ) {
        account = FakeAccountRepository().also { it.pairingSignInError = pairingSignInError }
        useCase = SignInWithWatchPairingUseCase(
            accountRepository = account,
            completeSignIn = {
                completeSignInCount++
                completeSignInResult
            },
        )
    }

    private companion object {
        const val TOKEN_HASH = "token-hash"
    }
}

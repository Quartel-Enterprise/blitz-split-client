package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.TooManyWatchPairingAttemptsException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ApproveWatchPairingUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApproveWatchPairingUseCaseTest {
    private lateinit var repository: ApprovingWatchPairingRepository
    private lateinit var useCase: ApproveWatchPairingUseCase

    @Test
    fun `GIVEN a local only profile WHEN approving a watch code THEN asks for an account without calling the server`() =
        runTest {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            val result = useCase(CODE)

            // Then
            assertIs<WatchPairingNeedsAccountException>(result.exceptionOrNull())
            assertTrue(repository.approvedCodes.isEmpty())
        }

    @Test
    fun `GIVEN an anonymous account WHEN approving a watch code THEN approves the code`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID))

        // When
        val result = useCase(CODE)

        // Then
        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(CODE), repository.approvedCodes)
    }

    @Test
    fun `GIVEN a Google account WHEN approving a watch code THEN approves the code`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        )

        // When
        val result = useCase(CODE)

        // Then
        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(CODE), repository.approvedCodes)
    }

    @Test
    fun `GIVEN an Apple account WHEN approving a watch code THEN approves the code`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE),
        )

        // When
        val result = useCase(CODE)

        // Then
        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(CODE), repository.approvedCodes)
    }

    @Test
    fun `GIVEN a code typed in lower case with a space WHEN approving it THEN sends the code as the server reads it`() =
        runTest {
            // Given
            prepareScenario()
            val typed = "k7p 4qx"

            // When
            val result = useCase(typed)

            // Then
            assertEquals(Result.success(Unit), result)
            assertEquals(listOf(CODE), repository.approvedCodes)
        }

    @Test
    fun `GIVEN a code with a letter missing WHEN approving it THEN fails as invalid without calling the server`() =
        runTest {
            // Given
            prepareScenario()
            val typed = "K7P 4Q"

            // When
            val result = useCase(typed)

            // Then
            assertIs<InvalidWatchPairingCodeException>(result.exceptionOrNull())
            assertTrue(repository.approvedCodes.isEmpty())
        }

    @Test
    fun `GIVEN a code nobody asked for WHEN approving it THEN fails as invalid`() = runTest {
        // Given
        prepareScenario(approveResult = Result.failure(InvalidWatchPairingCodeException()))

        // When
        val result = useCase(CODE)

        // Then
        assertIs<InvalidWatchPairingCodeException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a code whose 5 minutes passed WHEN approving it THEN fails as expired`() = runTest {
        // Given
        prepareScenario(approveResult = Result.failure(ExpiredWatchPairingException()))

        // When
        val result = useCase(CODE)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN too many wrong codes typed WHEN approving a code THEN fails with too many attempts`() = runTest {
        // Given
        prepareScenario(approveResult = Result.failure(TooManyWatchPairingAttemptsException()))

        // When
        val result = useCase(CODE)

        // Then
        assertIs<TooManyWatchPairingAttemptsException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        status: AccountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
        approveResult: Result<Unit> = Result.success(Unit),
    ) {
        repository = ApprovingWatchPairingRepository(approveResult)
        useCase = ApproveWatchPairingUseCase(
            accountRepository = FakeAccountRepository(initialStatus = status),
            repository = repository,
        )
    }

    private companion object {
        const val CODE = "K7P4QX"
    }
}

private class ApprovingWatchPairingRepository(
    private val approveResult: Result<Unit>,
) : WatchPairingRepository {
    val approvedCodes = mutableListOf<String>()

    override suspend fun requestCode(): Result<WatchPairingCode> = error("unused")

    override suspend fun requestApproval(code: String): Result<Unit> {
        approvedCodes += code
        return approveResult
    }

    override suspend fun fetchStatus(secret: String): Result<WatchPairingStatus> = error("unused")
}

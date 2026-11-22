package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.impl.AwaitWatchPairingUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AwaitWatchPairingUseCaseTest {
    private lateinit var repository: PollingWatchPairingRepository
    private lateinit var useCase: AwaitWatchPairingUseCase

    @Test
    fun `GIVEN the code is already approved WHEN awaiting the approval THEN returns the token hash at once`() =
        runTest {
            // Given
            prepareScenario(polls = listOf(approved))

            // When
            val result = useCase(code)

            // Then
            assertEquals(TOKEN_HASH, result.getOrNull())
            assertEquals(listOf(0L), repository.pollTimes)
            assertEquals(listOf(SECRET), repository.polledSecrets)
        }

    @Test
    fun `GIVEN a later approval WHEN awaiting the approval THEN polls once per interval until approved`() = runTest {
        // Given
        prepareScenario(polls = listOf(pending, pending, approved))

        // When
        val result = useCase(code)

        // Then
        assertEquals(TOKEN_HASH, result.getOrNull())
        assertEquals(listOf(0L, 2_000L, 4_000L), repository.pollTimes)
    }

    @Test
    fun `GIVEN nobody approves the code WHEN awaiting the approval THEN fails as expired once it expires`() = runTest {
        // Given
        prepareScenario(polls = emptyList())

        // When
        val result = useCase(code)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
        assertEquals(listOf(0L, 2_000L, 4_000L, 6_000L, 8_000L), repository.pollTimes)
        assertTrue(repository.pollTimes.all { it < EXPIRES_AT })
    }

    @Test
    fun `GIVEN the watch loses its connection WHEN awaiting the approval THEN polls again after the interval`() =
        runTest {
            // Given
            prepareScenario(polls = listOf(Result.failure(IllegalStateException("no answer")), approved))

            // When
            val result = useCase(code)

            // Then
            assertEquals(TOKEN_HASH, result.getOrNull())
            assertEquals(listOf(0L, 2_000L), repository.pollTimes)
        }

    @Test
    fun `GIVEN the server says the code expired WHEN awaiting the approval THEN fails as expired at once`() = runTest {
        // Given
        prepareScenario(polls = listOf(Result.failure(ExpiredWatchPairingException()), approved))

        // When
        val result = useCase(code)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
        assertEquals(listOf(0L), repository.pollTimes)
    }

    @Test
    fun `GIVEN the server forgot the secret WHEN awaiting the approval THEN fails as expired at once`() = runTest {
        // Given
        prepareScenario(polls = listOf(Result.failure(InvalidWatchPairingCodeException()), approved))

        // When
        val result = useCase(code)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
        assertEquals(listOf(0L), repository.pollTimes)
    }

    @Test
    fun `GIVEN a code that already expired WHEN awaiting the approval THEN fails as expired without polling`() =
        runTest {
            // Given
            prepareScenario(polls = listOf(approved))
            val expiredCode = code.copy(expiresAt = 0L)

            // When
            val result = useCase(expiredCode)

            // Then
            assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
            assertTrue(repository.pollTimes.isEmpty())
        }

    private val code = WatchPairingCode(
        code = "K7P4QX",
        secret = SECRET,
        expiresAt = EXPIRES_AT,
        pollInterval = 2.seconds,
    )
    private val pending: Result<WatchPairingStatus> = Result.success(WatchPairingStatus.Pending)
    private val approved: Result<WatchPairingStatus> = Result.success(WatchPairingStatus.Approved(TOKEN_HASH))

    private fun TestScope.prepareScenario(polls: List<Result<WatchPairingStatus>>) {
        repository = PollingWatchPairingRepository(
            polls = polls,
            now = { testScheduler.currentTime },
        )
        useCase = AwaitWatchPairingUseCase(
            repository = repository,
            currentTimeMillis = { testScheduler.currentTime },
        )
    }

    private companion object {
        const val SECRET = "watch-secret"
        const val TOKEN_HASH = "token-hash"
        const val EXPIRES_AT = 10_000L
    }
}

// Why: answers each poll with the next result, then pending forever, and records when each poll happened.
private class PollingWatchPairingRepository(
    private val now: () -> Long,
    polls: List<Result<WatchPairingStatus>>,
) : WatchPairingRepository {
    private val remaining = ArrayDeque(polls)
    val pollTimes = mutableListOf<Long>()
    val polledSecrets = mutableListOf<String>()

    override suspend fun requestCode(): Result<WatchPairingCode> = error("unused")

    override suspend fun requestApproval(code: String): Result<Unit> = error("unused")

    override suspend fun fetchStatus(secret: String): Result<WatchPairingStatus> {
        pollTimes += now()
        polledSecrets += secret
        return remaining.removeFirstOrNull() ?: Result.success(WatchPairingStatus.Pending)
    }
}

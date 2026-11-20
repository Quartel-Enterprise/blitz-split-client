package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.impl.RequestWatchPairingUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class RequestWatchPairingUseCaseTest {
    private lateinit var useCase: RequestWatchPairingUseCase

    @Test
    fun `GIVEN the server hands out a code WHEN the watch asks for one THEN returns that code`() = runTest {
        // Given
        prepareScenario(requested = Result.success(code))

        // When
        val result = useCase()

        // Then
        assertEquals(code, result.getOrNull())
    }

    @Test
    fun `GIVEN the watch is offline WHEN it asks for a code THEN fails`() = runTest {
        // Given
        prepareScenario(requested = Result.failure(IllegalStateException("offline")))

        // When
        val result = useCase()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val code = WatchPairingCode(
        code = "K7P4QX",
        secret = "watch-secret",
        expiresAt = 300_000L,
        pollInterval = 2.seconds,
    )

    private fun prepareScenario(requested: Result<WatchPairingCode>) {
        useCase = RequestWatchPairingUseCase(repository = CodeOnlyWatchPairingRepository(requested))
    }
}

private class CodeOnlyWatchPairingRepository(
    private val requested: Result<WatchPairingCode>,
) : WatchPairingRepository {
    override suspend fun requestCode(): Result<WatchPairingCode> = requested

    override suspend fun requestApproval(code: String): Result<Unit> = error("unused")

    override suspend fun fetchStatus(secret: String): Result<WatchPairingStatus> = error("unused")
}

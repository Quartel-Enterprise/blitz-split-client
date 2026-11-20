package com.quare.blitzsplit.core.watch.data

import com.quare.blitzsplit.core.watch.data.datasource.WatchPairingRemoteDataSource
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingCodeDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingPollResponseDto
import com.quare.blitzsplit.core.watch.data.repository.WatchPairingRepositoryImpl
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.TooManyWatchPairingAttemptsException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class WatchPairingRepositoryImplTest {
    private lateinit var remote: FakeWatchPairingRemoteDataSource
    private lateinit var repository: WatchPairingRepositoryImpl

    @Test
    fun `GIVEN the server hands out a code WHEN requesting it THEN expires 5 minutes later on the watch clock`() =
        runTest {
            // Given
            prepareScenario(
                codeDto = codeDto(
                    expiresAt = "2026-11-20T12:05:00.123Z",
                    pollIntervalSeconds = 3,
                ),
            )

            // When
            val result = repository.requestCode()

            // Then
            assertEquals(
                WatchPairingCode(
                    code = CODE,
                    secret = SECRET,
                    expiresAt = NOW + 300_000L,
                    pollInterval = 3.seconds,
                ),
                result.getOrNull(),
            )
        }

    @Test
    fun `GIVEN the server asks for a poll every 0 seconds WHEN requesting a code THEN polls every second`() = runTest {
        // Given
        prepareScenario(codeDto = codeDto(pollIntervalSeconds = 0))

        // When
        val result = repository.requestCode()

        // Then
        assertEquals(1.seconds, result.getOrNull()?.pollInterval)
    }

    @Test
    fun `GIVEN the server is unreachable WHEN requesting a code THEN fails`() = runTest {
        // Given
        prepareScenario(requestError = IllegalStateException("offline"))

        // When
        val result = repository.requestCode()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a code the server knows WHEN approving it THEN sends the code and succeeds`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.requestApproval(CODE)

        // Then
        assertEquals(Result.success(Unit), result)
        assertEquals(listOf(CODE), remote.approvedCodes)
    }

    @Test
    fun `GIVEN a code nobody asked for WHEN approving it THEN fails with an invalid code`() = runTest {
        // Given
        prepareScenario(approveError = InvalidWatchPairingCodeException())

        // When
        val result = repository.requestApproval(CODE)

        // Then
        assertIs<InvalidWatchPairingCodeException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a code whose 5 minutes passed WHEN approving it THEN fails as expired`() = runTest {
        // Given
        prepareScenario(approveError = ExpiredWatchPairingException())

        // When
        val result = repository.requestApproval(CODE)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN too many wrong codes typed WHEN approving a code THEN fails with too many attempts`() = runTest {
        // Given
        prepareScenario(approveError = TooManyWatchPairingAttemptsException())

        // When
        val result = repository.requestApproval(CODE)

        // Then
        assertIs<TooManyWatchPairingAttemptsException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN nobody approved the code yet WHEN polling THEN is pending and sends the secret`() = runTest {
        // Given
        prepareScenario(
            pollResponse = WatchPairingPollResponseDto(
                status = "pending",
                tokenHash = null,
            ),
        )

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertEquals(WatchPairingStatus.Pending, result.getOrNull())
        assertEquals(listOf(SECRET), remote.polledSecrets)
    }

    @Test
    fun `GIVEN a device approved the code WHEN polling THEN is approved with the token hash`() = runTest {
        // Given
        prepareScenario(
            pollResponse = WatchPairingPollResponseDto(
                status = "approved",
                tokenHash = TOKEN_HASH,
            ),
        )

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertEquals(WatchPairingStatus.Approved(TOKEN_HASH), result.getOrNull())
    }

    @Test
    fun `GIVEN an approval without a token hash WHEN polling THEN is still pending`() = runTest {
        // Given
        prepareScenario(
            pollResponse = WatchPairingPollResponseDto(
                status = "approved",
                tokenHash = null,
            ),
        )

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertEquals(WatchPairingStatus.Pending, result.getOrNull())
    }

    @Test
    fun `GIVEN a status the app does not know WHEN polling THEN is pending`() = runTest {
        // Given
        prepareScenario(
            pollResponse = WatchPairingPollResponseDto(
                status = "something-new",
                tokenHash = TOKEN_HASH,
            ),
        )

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertEquals(WatchPairingStatus.Pending, result.getOrNull())
    }

    @Test
    fun `GIVEN the code expired WHEN polling THEN fails as expired`() = runTest {
        // Given
        prepareScenario(pollError = ExpiredWatchPairingException())

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertIs<ExpiredWatchPairingException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a secret the server does not know WHEN polling THEN fails with an invalid code`() = runTest {
        // Given
        prepareScenario(pollError = InvalidWatchPairingCodeException())

        // When
        val result = repository.fetchStatus(SECRET)

        // Then
        assertIs<InvalidWatchPairingCodeException>(result.exceptionOrNull())
    }

    private fun codeDto(
        expiresAt: String = "2026-11-20T12:05:00Z",
        pollIntervalSeconds: Int = 2,
    ): WatchPairingCodeDto = WatchPairingCodeDto(
        code = CODE,
        secret = SECRET,
        expiresAt = expiresAt,
        pollIntervalSeconds = pollIntervalSeconds,
    )

    private fun prepareScenario(
        codeDto: WatchPairingCodeDto = codeDto(),
        requestError: Exception? = null,
        approveError: Exception? = null,
        pollResponse: WatchPairingPollResponseDto = WatchPairingPollResponseDto(
            status = "pending",
            tokenHash = null,
        ),
        pollError: Exception? = null,
    ) {
        remote = FakeWatchPairingRemoteDataSource(
            codeDto = codeDto,
            requestError = requestError,
            approveError = approveError,
            pollResponse = pollResponse,
            pollError = pollError,
        )
        repository = WatchPairingRepositoryImpl(
            remote = remote,
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val CODE = "K7P4QX"
        const val NOW = 1_795_176_000_000L
        const val SECRET = "watch-secret"
        const val TOKEN_HASH = "token-hash"
    }
}

private class FakeWatchPairingRemoteDataSource(
    private val codeDto: WatchPairingCodeDto,
    private val requestError: Exception?,
    private val approveError: Exception?,
    private val pollResponse: WatchPairingPollResponseDto,
    private val pollError: Exception?,
) : WatchPairingRemoteDataSource {
    val approvedCodes = mutableListOf<String>()
    val polledSecrets = mutableListOf<String>()

    override suspend fun requestCode(): WatchPairingCodeDto {
        requestError?.let { throw it }
        return codeDto
    }

    override suspend fun approve(code: String) {
        approvedCodes += code
        approveError?.let { throw it }
    }

    override suspend fun fetchStatus(secret: String): WatchPairingPollResponseDto {
        polledSecrets += secret
        pollError?.let { throw it }
        return pollResponse
    }
}

package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ApproveNearbyWatchUseCase
import com.quare.blitzsplit.core.watch.link.WatchLink
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApproveNearbyWatchUseCaseTest {
    private lateinit var useCase: ApproveNearbyWatchUseCase

    @Test
    fun `GIVEN the server approves WHEN a nearby watch sends its code THEN answers approved`() = runTest {
        // Given
        prepareScenario(Result.success(Unit))

        // When
        val reply = useCase("K7P4QX")

        // Then
        assertEquals(WatchLink.REPLY_APPROVED, reply)
    }

    @Test
    fun `GIVEN a local only profile WHEN a nearby watch sends its code THEN answers local only`() = runTest {
        // Given
        prepareScenario(Result.failure(WatchPairingNeedsAccountException()))

        // When
        val reply = useCase("K7P4QX")

        // Then
        assertEquals(WatchLink.REPLY_LOCAL_ONLY, reply)
    }

    @Test
    fun `GIVEN the server refuses the code WHEN a nearby watch sends it THEN answers failed`() = runTest {
        // Given
        prepareScenario(Result.failure(InvalidWatchPairingCodeException()))

        // When
        val reply = useCase("K7P4QX")

        // Then
        assertEquals(WatchLink.REPLY_FAILED, reply)
    }

    private fun prepareScenario(approval: Result<Unit>) {
        useCase = ApproveNearbyWatchUseCase(approveWatchPairing = { approval })
    }
}

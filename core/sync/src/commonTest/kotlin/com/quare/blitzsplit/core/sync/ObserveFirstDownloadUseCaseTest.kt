package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveFirstDownloadUseCaseTest {
    private val firstDownload = FirstDownload()
    private val useCase = ObserveFirstDownloadUseCase(firstDownload)

    @Test
    fun `GIVEN a first download started WHEN observing THEN it is pending`() = runTest {
        // Given
        firstDownload.start()

        // When
        val isPending = useCase().first()

        // Then
        assertTrue(isPending)
    }

    @Test
    fun `GIVEN a first download started and ended WHEN observing THEN it is not pending`() = runTest {
        // Given
        firstDownload.start()
        firstDownload.end()

        // When
        val isPending = useCase().first()

        // Then
        assertFalse(isPending)
    }

    @Test
    fun `GIVEN nothing restored WHEN observing THEN no first download is pending`() = runTest {
        // Given
        val freshFirstDownload = FirstDownload()

        // When
        val isPending = ObserveFirstDownloadUseCase(freshFirstDownload)().first()

        // Then
        assertFalse(isPending)
    }
}

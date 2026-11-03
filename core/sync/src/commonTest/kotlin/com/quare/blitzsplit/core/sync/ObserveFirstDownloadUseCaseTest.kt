package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveFirstDownloadUseCaseTest {
    private val firstDownload = FirstDownload()
    private val useCase = ObserveFirstDownloadUseCase(firstDownload)

    @Test
    fun `GIVEN a first download started WHEN observing THEN no group came down yet`() = runTest {
        // Given
        firstDownload.start()

        // When
        val progress = useCase().first()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 0,
                groupCount = null,
            ),
            progress,
        )
    }

    @Test
    fun `GIVEN a first download started and ended WHEN observing THEN it is not pending`() = runTest {
        // Given
        firstDownload.start()
        firstDownload.end()

        // When
        val progress = useCase().first()

        // Then
        assertNull(progress)
    }

    @Test
    fun `GIVEN nothing restored WHEN observing THEN no first download is pending`() = runTest {
        // Given
        val freshFirstDownload = FirstDownload()

        // When
        val progress = ObserveFirstDownloadUseCase(freshFirstDownload)().first()

        // Then
        assertNull(progress)
    }
}

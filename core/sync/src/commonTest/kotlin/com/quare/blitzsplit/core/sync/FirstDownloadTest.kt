package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirstDownloadTest {
    private val firstDownload = FirstDownload()

    @Test
    fun `GIVEN nothing restored WHEN reading the first download THEN there is no progress and it is not pending`() {
        // Given
        val freshFirstDownload = FirstDownload()

        // When
        val progress = freshFirstDownload.observeProgress().value

        // Then
        assertNull(progress)
        assertFalse(freshFirstDownload.isPending)
    }

    @Test
    fun `GIVEN an account restored WHEN the first download starts THEN no group came down out of an unknown count`() {
        // Given
        val restored = firstDownload

        // When
        restored.start()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 0,
                groupCount = null,
                isWaitingForInternet = false,
            ),
            restored.observeProgress().value,
        )
        assertTrue(restored.isPending)
    }

    @Test
    fun `GIVEN a first download started WHEN the server says 7 groups are coming THEN counts 0 of 7`() {
        // Given
        firstDownload.start()
        val groupCount = 7

        // When
        firstDownload.expect(groupCount)

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 0,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
            firstDownload.observeProgress().value,
        )
        assertTrue(firstDownload.isPending)
    }

    @Test
    fun `GIVEN 7 groups expected and 2 already arrived WHEN another group arrives THEN counts 3 of 7`() {
        // Given
        firstDownload.start()
        firstDownload.expect(7)
        firstDownload.groupArrived()
        firstDownload.groupArrived()

        // When
        firstDownload.groupArrived()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 3,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
            firstDownload.observeProgress().value,
        )
    }

    @Test
    fun `GIVEN no first download started WHEN groups are expected THEN no progress appears`() {
        // Given
        val groupCount = 7

        // When
        firstDownload.expect(groupCount)

        // Then
        assertNull(firstDownload.observeProgress().value)
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN no first download started WHEN a group arrives THEN no progress appears`() {
        // Given
        val notStarted = firstDownload

        // When
        notStarted.groupArrived()

        // Then
        assertNull(notStarted.observeProgress().value)
        assertFalse(notStarted.isPending)
    }

    @Test
    fun `GIVEN a first download halfway WHEN it ends THEN there is no progress and it is not pending`() {
        // Given
        firstDownload.start()
        firstDownload.expect(7)
        firstDownload.groupArrived()

        // When
        firstDownload.end()

        // Then
        assertNull(firstDownload.observeProgress().value)
        assertFalse(firstDownload.isPending)
    }

    @Test
    fun `GIVEN a first download that ended WHEN a late group arrives THEN no progress comes back`() {
        // Given
        firstDownload.start()
        firstDownload.expect(7)
        firstDownload.end()

        // When
        firstDownload.groupArrived()

        // Then
        assertNull(firstDownload.observeProgress().value)
        assertFalse(firstDownload.isPending)
    }
}

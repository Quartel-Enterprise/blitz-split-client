package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.feature.groups.presentation.mapper.toDownloadProgress
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadProgressTest {
    @Test
    fun `GIVEN the server has not said how many groups are coming WHEN drawing the bar THEN it has no share`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 0,
            groupCount = null,
            isWaitingForInternet = false,
        )

        // When
        val fraction = progress.fraction

        // Then
        assertNull(fraction)
    }

    @Test
    fun `GIVEN the server has not said how many groups are coming WHEN drawing the rows THEN holds three places`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 2,
            groupCount = null,
            isWaitingForInternet = false,
        )

        // When
        val placeholders = progress.placeholderCount

        // Then
        assertEquals(3, placeholders)
    }

    @Test
    fun `GIVEN no group is coming WHEN drawing the bar THEN it has no share and holds no place`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 0,
            groupCount = 0,
            isWaitingForInternet = false,
        )

        // When
        val fraction = progress.fraction

        // Then
        assertNull(fraction)
        assertEquals(0, progress.placeholderCount)
    }

    @Test
    fun `GIVEN 3 of 7 groups down WHEN drawing the bar THEN fills 3 sevenths`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 3,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val fraction = progress.fraction

        // Then
        assertEquals(3f / 7f, fraction)
    }

    @Test
    fun `GIVEN none of 7 groups down yet WHEN drawing the bar THEN it starts empty`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 0,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val fraction = progress.fraction

        // Then
        assertEquals(0f, fraction)
    }

    @Test
    fun `GIVEN every group down WHEN drawing the bar THEN it is full and holds no place`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 7,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val fraction = progress.fraction

        // Then
        assertEquals(1f, fraction)
        assertEquals(0, progress.placeholderCount)
    }

    @Test
    fun `GIVEN 4 groups still to come WHEN drawing the rows THEN holds at most three places`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 3,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val placeholders = progress.placeholderCount

        // Then
        assertEquals(3, placeholders)
    }

    @Test
    fun `GIVEN 2 groups still to come WHEN drawing the rows THEN holds two places`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 5,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val placeholders = progress.placeholderCount

        // Then
        assertEquals(2, placeholders)
    }

    @Test
    fun `GIVEN more groups down than the server counted WHEN drawing the rows THEN holds no place`() {
        // Given
        val progress = DownloadProgress(
            arrivedGroupCount = 8,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val placeholders = progress.placeholderCount

        // Then
        assertEquals(0, placeholders)
    }

    @Test
    fun `GIVEN a download relaunched midway offline WHEN the home reads it THEN waits for the internet uncounted`() {
        // Given
        val progress = FirstDownloadProgress(
            arrivedGroupCount = 0,
            groupCount = null,
            isWaitingForInternet = true,
        )

        // When
        val shown = progress.toDownloadProgress()

        // Then
        assertEquals(
            DownloadProgress(
                arrivedGroupCount = 0,
                groupCount = null,
                isWaitingForInternet = true,
            ),
            shown,
        )
        assertNull(shown.fraction)
    }

    @Test
    fun `GIVEN 3 of 7 groups down online WHEN the home reads it THEN counts them without waiting`() {
        // Given
        val progress = FirstDownloadProgress(
            arrivedGroupCount = 3,
            groupCount = 7,
            isWaitingForInternet = false,
        )

        // When
        val shown = progress.toDownloadProgress()

        // Then
        assertEquals(
            DownloadProgress(
                arrivedGroupCount = 3,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
            shown,
        )
    }
}

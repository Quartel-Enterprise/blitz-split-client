package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class HomePeopleLayoutTest {
    private val phoneUpright = 360f
    private val phoneLyingDown = 800f
    private val tablet = 1200f

    @Test
    fun `GIVEN a phone upright and two people WHEN laying them out THEN shows them as rows in one column`() {
        // Given
        val peopleCount = 2

        // When
        val layout = HomePeopleLayout.of(peopleCount, phoneUpright)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 1,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a phone upright and one person WHEN laying them out THEN shows them as a row`() {
        // Given
        val peopleCount = 1

        // When
        val layout = HomePeopleLayout.of(peopleCount, phoneUpright)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 1,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a phone upright and three people WHEN laying them out THEN shows the row of faces`() {
        // Given
        val peopleCount = 3

        // When
        val layout = HomePeopleLayout.of(peopleCount, phoneUpright)

        // Then
        assertEquals(false, layout.showsRows)
    }

    @Test
    fun `GIVEN a phone lying down and four people WHEN laying them out THEN shows them as rows in two columns`() {
        // Given
        val peopleCount = 4

        // When
        val layout = HomePeopleLayout.of(peopleCount, phoneLyingDown)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 2,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a phone lying down and five people WHEN laying them out THEN shows the row of faces`() {
        // Given
        val peopleCount = 5

        // When
        val layout = HomePeopleLayout.of(peopleCount, phoneLyingDown)

        // Then
        assertEquals(false, layout.showsRows)
    }

    @Test
    fun `GIVEN a tablet and eight people WHEN laying them out THEN shows them as rows in four columns`() {
        // Given
        val peopleCount = 8

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 4,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a tablet and nine people WHEN laying them out THEN shows the row of faces`() {
        // Given
        val peopleCount = 9

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(false, layout.showsRows)
    }

    @Test
    fun `GIVEN a tablet and one person WHEN laying them out THEN shows one row in one column`() {
        // Given
        val peopleCount = 1

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 1,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a tablet and three people WHEN laying them out THEN shows the rows in three columns`() {
        // Given
        val peopleCount = 3

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 3,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a width of exactly two rows WHEN laying out four people THEN shows them as rows in two columns`() {
        // Given
        val sectionWidth = 2 * HomePeopleLayout.ROW_MIN_WIDTH

        // When
        val layout = HomePeopleLayout.of(4, sectionWidth)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 2,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a width just under one row WHEN laying out two people THEN still shows them as rows in one column`() {
        // Given
        val sectionWidth = HomePeopleLayout.ROW_MIN_WIDTH - 1f

        // When
        val layout = HomePeopleLayout.of(2, sectionWidth)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 1,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN a width not measured yet WHEN laying out people THEN behaves as one column`() {
        // Given
        val sectionWidth = 0f

        // When
        val twoPeople = HomePeopleLayout.of(2, sectionWidth)
        val threePeople = HomePeopleLayout.of(3, sectionWidth)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = true,
                columns = 1,
            ),
            twoPeople,
        )
        assertEquals(false, threePeople.showsRows)
    }

    @Test
    fun `GIVEN nobody WHEN laying them out THEN shows no rows and one column`() {
        // Given
        val peopleCount = 0

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = false,
                columns = 1,
            ),
            layout,
        )
    }

    @Test
    fun `GIVEN many people on a wide screen WHEN laying them out THEN the columns are those that fit`() {
        // Given
        val peopleCount = 20

        // When
        val layout = HomePeopleLayout.of(peopleCount, tablet)

        // Then
        assertEquals(
            HomePeopleLayout(
                showsRows = false,
                columns = 4,
            ),
            layout,
        )
    }
}

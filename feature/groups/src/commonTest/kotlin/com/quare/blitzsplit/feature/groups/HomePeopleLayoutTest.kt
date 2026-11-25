package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout
import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleMode
import kotlin.test.Test
import kotlin.test.assertEquals

class HomePeopleLayoutTest {
    private val narrow = 559f
    private val wide = 560f
    private val phone = 343f
    private val desktop = 1000f

    @Test
    fun `GIVEN a narrow block on touch and three people WHEN laying them out THEN lists all three`() {
        // Given
        val peopleCount = 3
        val blockWidth = narrow
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.LIST, layout.mode)
        assertEquals(3, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a narrow block on touch and four people WHEN laying them out THEN shows everyone in a carousel`() {
        // Given
        val peopleCount = 4
        val blockWidth = narrow
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CAROUSEL, layout.mode)
        assertEquals(4, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a narrow block with a pointer and three people WHEN laying them out THEN lists all three`() {
        // Given
        val peopleCount = 3
        val blockWidth = narrow
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.LIST, layout.mode)
        assertEquals(3, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a narrow block with a pointer and four people WHEN laying them out THEN lists three and hides one`() {
        // Given
        val peopleCount = 4
        val blockWidth = narrow
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.LIST, layout.mode)
        assertEquals(3, layout.shownCount)
        assertEquals(1, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a narrow block with a pointer and nine people WHEN laying them out THEN hides six`() {
        // Given
        val peopleCount = 9
        val blockWidth = narrow
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(3, layout.shownCount)
        assertEquals(6, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a wide block on touch and one person WHEN laying them out THEN shows a fixed card`() {
        // Given
        val peopleCount = 1
        val blockWidth = wide
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(1, layout.shownCount)
        assertEquals(HomePeopleLayout.CARD_WIDTH, layout.cardWidth)
    }

    @Test
    fun `GIVEN a wide block on touch and three people WHEN laying them out THEN shows fixed cards`() {
        // Given
        val peopleCount = 3
        val blockWidth = wide
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(3, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a wide block with a pointer and three people WHEN laying them out THEN shows fixed cards`() {
        // Given
        val peopleCount = 3
        val blockWidth = wide
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(3, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a desktop block and one person WHEN laying them out THEN the card keeps its fixed width`() {
        // Given
        val peopleCount = 1
        val blockWidth = 1440f
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(HomePeopleLayout.CARD_WIDTH, layout.cardWidth)
    }

    @Test
    fun `GIVEN a wide block on touch and four people WHEN laying them out THEN shows everyone in a carousel`() {
        // Given
        val peopleCount = 4
        val blockWidth = wide
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CAROUSEL, layout.mode)
        assertEquals(4, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a wide block with a pointer and four people WHEN laying them out THEN shows them all as cards`() {
        // Given
        val peopleCount = 4
        val blockWidth = wide
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(2, layout.columns)
        assertEquals(4, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a wide block with a pointer and nine people WHEN laying them out THEN keeps a slot for the tile`() {
        // Given
        val peopleCount = 9
        val blockWidth = wide
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CARDS, layout.mode)
        assertEquals(2, layout.columns)
        assertEquals(3, layout.shownCount)
        assertEquals(6, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a desktop block with a pointer and nine people WHEN laying them out THEN shows seven and the tile`() {
        // Given
        val peopleCount = 9
        val blockWidth = desktop
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(4, layout.columns)
        assertEquals(7, layout.shownCount)
        assertEquals(2, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a phone on touch and four people WHEN laying them out THEN about two and a half cards are in sight`() {
        // Given
        val peopleCount = 4
        val blockWidth = phone
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        val inSight = (blockWidth + HomePeopleLayout.CARD_GAP) / (layout.cardWidth + HomePeopleLayout.CARD_GAP)
        assertEquals(true, inSight > 2f && inSight < 3f)
    }

    @Test
    fun `GIVEN a very wide touch block WHEN laying out a carousel THEN its cards keep the card width`() {
        // Given
        val peopleCount = 9
        val blockWidth = desktop
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleLayout.CARD_WIDTH, layout.cardWidth)
    }

    @Test
    fun `GIVEN a tiny touch block WHEN laying out a carousel THEN its cards keep their narrowest width`() {
        // Given
        val peopleCount = 4
        val blockWidth = 200f
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleLayout.CAROUSEL_CARD_MIN_WIDTH, layout.cardWidth)
    }

    @Test
    fun `GIVEN a width not measured yet on touch WHEN laying out four people THEN shows the carousel`() {
        // Given
        val peopleCount = 4
        val blockWidth = 0f
        val isTouch = true

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.CAROUSEL, layout.mode)
    }

    @Test
    fun `GIVEN a width not measured yet with a pointer WHEN laying out two people THEN lists them`() {
        // Given
        val peopleCount = 2
        val blockWidth = 0f
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(HomePeopleMode.LIST, layout.mode)
    }

    @Test
    fun `GIVEN nobody WHEN laying them out THEN shows nobody`() {
        // Given
        val peopleCount = 0
        val blockWidth = desktop
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(0, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN a wide block with a pointer and exactly two lines of people WHEN laying them out THEN shows everyone`() {
        // Given
        val peopleCount = 10
        val blockWidth = 1038f
        val isTouch = false

        // When
        val layout = HomePeopleLayout.of(
            peopleCount = peopleCount,
            blockWidth = blockWidth,
            isTouch = isTouch,
        )

        // Then
        assertEquals(5, layout.columns)
        assertEquals(10, layout.shownCount)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `GIVEN nine people in two lines of three with a pointer WHEN reading the lines THEN the tile ends them`() {
        // Given
        val layout = HomePeopleLayout.of(
            peopleCount = 9,
            blockWidth = 655f,
            isTouch = false,
        )

        // When
        val lineSizes = layout.lineSizes

        // Then
        assertEquals(listOf(3, 3), lineSizes)
        assertEquals(5, layout.shownCount)
        assertEquals(true, layout.showsMore)
    }

    @Test
    fun `GIVEN three people on a wide block WHEN reading the lines THEN they fill lines of the columns`() {
        // Given
        val layout = HomePeopleLayout.of(
            peopleCount = 3,
            blockWidth = wide,
            isTouch = true,
        )

        // When
        val lineSizes = layout.lineSizes

        // Then
        assertEquals(listOf(2, 1), lineSizes)
        assertEquals(false, layout.showsMore)
    }

    @Test
    fun `GIVEN a carousel WHEN asking for the title count THEN it shows only while the carousel overflows`() {
        // Given
        val layout = HomePeopleLayout.of(
            peopleCount = 4,
            blockWidth = phone,
            isTouch = true,
        )

        // When
        val overflowing = layout.showsCountInTitle(carouselOverflows = true)
        val fitting = layout.showsCountInTitle(carouselOverflows = false)

        // Then
        assertEquals(true, overflowing)
        assertEquals(false, fitting)
    }

    @Test
    fun `GIVEN people left out WHEN asking for the title count THEN it shows`() {
        // Given
        val layout = HomePeopleLayout.of(
            peopleCount = 4,
            blockWidth = narrow,
            isTouch = false,
        )

        // When
        val counts = layout.showsCountInTitle(carouselOverflows = false)

        // Then
        assertEquals(true, counts)
    }

    @Test
    fun `GIVEN cards that all fit WHEN asking for the title count THEN it does not show even if something overflows`() {
        // Given
        val layout = HomePeopleLayout.of(
            peopleCount = 3,
            blockWidth = desktop,
            isTouch = false,
        )

        // When
        val counts = layout.showsCountInTitle(carouselOverflows = true)

        // Then
        assertEquals(false, counts)
    }
}

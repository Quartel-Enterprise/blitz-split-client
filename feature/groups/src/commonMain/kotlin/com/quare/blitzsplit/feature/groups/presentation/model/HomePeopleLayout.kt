package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: how Início's "Saldos com pessoas" draws its people, the same on the three UIs. It used to stretch a list row
 * across the whole block, so on a wide web window one person's name sat ~800px away from their amount. The mode now
 * comes from three things only: the width the block itself gets (dp on Android, pt on Apple, CSS px on the web, never
 * the device's), whether the input is touch (Android and iOS always, the web by `(pointer: coarse)`) or a pointer (the
 * Mac always, the web with a mouse), and how many people there are:
 *
 * | Block       | Input   | 1–3 people              | 4+ people                                               |
 * | < 560       | touch   | LIST                    | CAROUSEL, everyone, ~2.5 cards visible                  |
 * | < 560       | pointer | LIST                    | LIST of the first 3, then a "+N" row for the rest       |
 * | ≥ 560       | touch   | CARDS, start-aligned    | CAROUSEL                                                |
 * | ≥ 560       | pointer | CARDS, start-aligned    | CARDS, at most 2 lines, the last slot a "+N" tile       |
 *
 * Cards never stretch: they keep CARD_WIDTH and the space after them stays empty on purpose. A list row is never wider
 * than 560: LIST only comes below WIDE_BLOCK_MIN_WIDTH, so the threshold already caps it. The order is the ViewModel's (whom you owe first, then who owes you, the largest first).
 */
data class HomePeopleLayout(
    val mode: HomePeopleMode,
    /*
     * Why: how many people the UI draws, from the first; the rest are hiddenCount. While it is over 0 the UI draws the
     * hidden people's faces and "+N" after the shown ones, which opens Balances: a row of the list, or a tile in the
     * cards' last slot (shownCount leaves that slot free). The header's "See balances" stays the only link of words.
     */
    val shownCount: Int,
    val hiddenCount: Int,
    // Why: CARDS only, how many cards fit on a line; the UI lays the shown ones in lines of this many.
    val columns: Int,
    // Why: CARDS and CAROUSEL, each card's fixed width.
    val cardWidth: Float,
) {
    // Why: the "+N" row or tile follows the shown people while someone is left out.
    val showsMore: Boolean
        get() = hiddenCount > 0

    /*
     * Why: CARDS, how many slots each line holds, the "+N" tile counted in the last one; the UIs fill the lines in
     * order, the shown people first, and draw nothing of their own about where a line breaks.
     */
    val lineSizes: List<Int>
        get() {
            val slots = shownCount + if (showsMore) 1 else 0
            return (0 until slots step columns).map { start -> minOf(columns, slots - start) }
        }

    // Why: the title's " · 9" says there are more people than in sight: a carousel that scrolls, or anyone left out.
    fun showsCountInTitle(carouselOverflows: Boolean): Boolean =
        (mode == HomePeopleMode.CAROUSEL && carouselOverflows) || showsMore

    companion object {
        // Why: the design system's column-android-portrait, the width a phone column reads well up to.
        const val WIDE_BLOCK_MIN_WIDTH = 560f

        const val MAX_LIST_PEOPLE = 3
        const val MAX_CARD_LINES = 2

        // Why: a card's width (face, name, amount and word stacked) in the 200–280 the design allows.
        const val CARD_WIDTH = 200f

        // Why: the narrowest a carousel card gets on a small phone, so a name of about eight letters still fits.
        const val CAROUSEL_CARD_MIN_WIDTH = 112f

        // Why: about two and a half cards in sight, so the cut one says the row scrolls.
        const val CAROUSEL_VISIBLE_CARDS = 2.5f

        // Why: the gap the three UIs leave between two cards, and between two lines of them.
        const val CARD_GAP = 8f

        fun of(
            peopleCount: Int,
            blockWidth: Float,
            isTouch: Boolean,
        ): HomePeopleLayout {
            val count = peopleCount.coerceAtLeast(0)
            val isWide = blockWidth >= WIDE_BLOCK_MIN_WIDTH
            val isFew = count <= MAX_LIST_PEOPLE
            return when {
                isFew && isWide -> buildCardsLayout(count, count, blockWidth)

                isFew -> buildListLayout(count, count)

                isTouch -> buildCarouselLayout(count, blockWidth)

                isWide -> buildCardsLayout(
                    count,
                    MAX_CARD_LINES * columnsOf(blockWidth),
                    blockWidth,
                    reservesTile = true,
                )

                else -> buildListLayout(count, MAX_LIST_PEOPLE)
            }
        }

        private fun buildListLayout(
            count: Int,
            limit: Int,
        ): HomePeopleLayout = buildLayout(
            HomePeopleMode.LIST,
            count,
            limit,
            columns = 1,
            cardWidth = CARD_WIDTH,
        )

        // Why: with reservesTile, when not everyone fits the "+N" tile takes the last slot, so one person fewer shows.
        private fun buildCardsLayout(
            count: Int,
            slots: Int,
            blockWidth: Float,
            reservesTile: Boolean = false,
        ): HomePeopleLayout = buildLayout(
            mode = HomePeopleMode.CARDS,
            count = count,
            limit = if (reservesTile && count > slots) slots - 1 else slots,
            columns = columnsOf(blockWidth),
            cardWidth = CARD_WIDTH,
        )

        private fun buildCarouselLayout(
            count: Int,
            blockWidth: Float,
        ): HomePeopleLayout {
            val fitting = (blockWidth - CARD_GAP * CAROUSEL_VISIBLE_CARDS.toInt()) / CAROUSEL_VISIBLE_CARDS
            return buildLayout(
                mode = HomePeopleMode.CAROUSEL,
                count = count,
                limit = count,
                columns = 1,
                cardWidth = fitting.coerceIn(CAROUSEL_CARD_MIN_WIDTH, CARD_WIDTH),
            )
        }

        private fun buildLayout(
            mode: HomePeopleMode,
            count: Int,
            limit: Int,
            columns: Int,
            cardWidth: Float,
        ): HomePeopleLayout {
            val shownCount = count.coerceAtMost(limit)
            return HomePeopleLayout(
                mode = mode,
                shownCount = shownCount,
                hiddenCount = count - shownCount,
                columns = columns,
                cardWidth = cardWidth,
            )
        }

        private fun columnsOf(blockWidth: Float): Int =
            ((blockWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP)).toInt().coerceAtLeast(1)
    }
}

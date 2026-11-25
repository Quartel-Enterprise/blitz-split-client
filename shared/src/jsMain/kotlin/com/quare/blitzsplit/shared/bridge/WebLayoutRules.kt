@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout

/*
 * Why: FaceStack's bubbles, the same rule Android and Apple draw a stack of faces by. size is "roomy" on a screen's own
 * header or body and "compact" in a list's row or a form's field, the web's words for FaceStack's entries.
 */
fun getStackedFaceCount(
    peopleCount: Int,
    size: String,
): Int = stackOf(size).faceCountOf(peopleCount)

fun getStackedExtraCount(
    peopleCount: Int,
    size: String,
): Int = stackOf(size).extraCountOf(peopleCount)

private fun stackOf(size: String): FaceStack =
    FaceStack.entries.firstOrNull { it.name.equals(size, ignoreCase = true) } ?: FaceStack.COMPACT

// Why: mode is HomePeopleMode's name, "LIST", "CAROUSEL" or "CARDS".
class JsHomePeopleLayout(
    val mode: String,
    val shownCount: Int,
    val hiddenCount: Int,
    val columns: Int,
    val cardWidth: Double,
    val cardGap: Double,
    // Why: CARDS, the slots of each line, the "+N" tile counted in the last one.
    val lineSizes: Array<Int>,
    val showsMore: Boolean,
    // Why: HomePeopleLayout.showsCountInTitle's answer when the carousel overflows its block, and when it doesn't.
    val countsInTitleIfOverflowing: Boolean,
    val countsInTitleOtherwise: Boolean,
)

// Why: Início's "Saldos com pessoas" as a list, a carousel or cards, by the block's width in CSS px, the input and the count.
fun getHomePeopleLayout(
    peopleCount: Int,
    blockWidth: Double,
    isTouch: Boolean,
): JsHomePeopleLayout {
    val layout = HomePeopleLayout.of(peopleCount, blockWidth.toFloat(), isTouch)
    return JsHomePeopleLayout(
        mode = layout.mode.name,
        shownCount = layout.shownCount,
        hiddenCount = layout.hiddenCount,
        columns = layout.columns,
        cardWidth = layout.cardWidth.toDouble(),
        cardGap = HomePeopleLayout.CARD_GAP.toDouble(),
        lineSizes = layout.lineSizes.toTypedArray(),
        showsMore = layout.showsMore,
        countsInTitleIfOverflowing = layout.showsCountInTitle(carouselOverflows = true),
        countsInTitleOtherwise = layout.showsCountInTitle(carouselOverflows = false),
    )
}

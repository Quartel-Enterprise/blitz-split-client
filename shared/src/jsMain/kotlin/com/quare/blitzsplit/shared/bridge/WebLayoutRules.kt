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

class JsHomePeopleLayout(
    val showsRows: Boolean,
    val columns: Int,
)

// Why: Início's "Saldos com pessoas" as rows or faces, by the section's width in CSS px and the count.
fun getHomePeopleLayout(
    peopleCount: Int,
    sectionWidth: Double,
): JsHomePeopleLayout {
    val layout = HomePeopleLayout.of(peopleCount, sectionWidth.toFloat())
    return JsHomePeopleLayout(
        showsRows = layout.showsRows,
        columns = layout.columns,
    )
}
